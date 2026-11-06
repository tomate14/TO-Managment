import { Component, inject, output, signal } from '@angular/core';
import { AbstractControl, FormControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { apiErrorCode, apiErrorMessage } from '../../../core/api/api-error';
import { ESTABLISHMENT_ERROR_CODES, ESTABLISHMENT_NAME_TAKEN_MESSAGE, EstablishmentSummary } from '../establishments.models';
import { EstablishmentsService } from '../establishments.service';

type FieldName = 'name' | 'fullName' | 'email' | 'password';

const MIN_PASSWORD = 8;
const MAX_PASSWORD = 72;
const MAX_NAME = 120;
const MAX_EMAIL = 254;
const SAVE_ERROR = 'No pudimos crear el establecimiento. Intentá de nuevo.';

/** Email format check on the trimmed value (mobile autocomplete adds trailing spaces). */
function trimmedEmail(control: AbstractControl<string>): ValidationErrors | null {
  return Validators.email(new FormControl(control.value.trim()));
}

/** Required check that treats whitespace-only text as empty. */
function notBlank(control: AbstractControl<string>): ValidationErrors | null {
  return control.value.trim() === '' ? { required: true } : null;
}

/** H8.1 form shared by the mobile page and the desktop side panel: establishment name + first admin. */
@Component({
  selector: 'app-establishment-form',
  imports: [ReactiveFormsModule],
  templateUrl: './establishment-form.html',
  styleUrl: './establishment-form.scss',
})
export class EstablishmentForm {
  private readonly establishmentsService = inject(EstablishmentsService);

  readonly created = output<EstablishmentSummary>();
  readonly cancelled = output<void>();

  protected readonly form = inject(NonNullableFormBuilder).group({
    name: ['', [notBlank, Validators.maxLength(MAX_NAME)]],
    fullName: ['', [notBlank, Validators.maxLength(MAX_NAME)]],
    email: ['', [notBlank, trimmedEmail, Validators.maxLength(MAX_EMAIL)]],
    password: ['', [Validators.required, Validators.minLength(MIN_PASSWORD), Validators.maxLength(MAX_PASSWORD)]],
  });

  protected readonly submitted = signal(false);
  protected readonly saving = signal(false);
  protected readonly nameTaken = signal(false);
  protected readonly emailTaken = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.form.controls.name.valueChanges.subscribe(() => this.nameTaken.set(false));
    this.form.controls.email.valueChanges.subscribe(() => this.emailTaken.set(false));
  }

  /** Message shown under a field, or null when it is valid or the user has not tried to save yet. */
  protected fieldError(field: FieldName): string | null {
    const control = this.form.controls[field];
    if (field === 'name' && this.nameTaken()) {
      return ESTABLISHMENT_NAME_TAKEN_MESSAGE;
    }
    if (field === 'email' && this.emailTaken()) {
      return 'Ya hay un usuario con este email.';
    }
    if ((!this.submitted() && !control.touched) || control.valid) {
      return null;
    }
    if (control.hasError('required')) {
      return {
        name: 'Ingresá el nombre del establecimiento.',
        fullName: 'Ingresá el nombre y apellido.',
        email: 'Ingresá el email.',
        password: 'Ingresá la contraseña inicial.',
      }[field];
    }
    if (control.hasError('email')) {
      return 'Ingresá un email válido, como nombre@consultorio.com.';
    }
    if (control.hasError('minlength')) {
      return `La contraseña tiene que tener al menos ${MIN_PASSWORD} caracteres.`;
    }
    return 'Es demasiado largo.';
  }

  protected submit(): void {
    if (this.saving()) {
      return;
    }
    this.submitted.set(true);
    this.error.set(null);
    if (this.form.invalid) {
      return;
    }
    this.saving.set(true);
    const { name, fullName, email, password } = this.form.getRawValue();
    this.establishmentsService
      .create({ name: name.trim(), admin: { fullName: fullName.trim(), email: email.trim(), password } })
      .subscribe({
        next: (establishment) => {
          this.saving.set(false);
          this.created.emit(establishment);
        },
        error: (err: unknown) => {
          this.saving.set(false);
          const code = apiErrorCode(err);
          if (code === ESTABLISHMENT_ERROR_CODES.nameTaken) {
            this.nameTaken.set(true);
          } else if (code === ESTABLISHMENT_ERROR_CODES.emailTaken) {
            this.emailTaken.set(true);
          } else {
            this.error.set(apiErrorMessage(err) ?? SAVE_ERROR);
          }
        },
      });
  }
}
