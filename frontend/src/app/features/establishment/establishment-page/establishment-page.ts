import { Component, OnInit, inject, signal } from '@angular/core';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { apiErrorCode, apiErrorMessage } from '../../../core/api/api-error';
import { AuthService } from '../../../core/auth/auth.service';
import { ESTABLISHMENT_ERROR_CODES, ESTABLISHMENT_NAME_TAKEN_MESSAGE } from '../../establishments/establishments.models';
import { formatDate } from '../../patients/patient-format';
import { CurrentEstablishment, CurrentEstablishmentService } from '../current-establishment.service';

const MAX_NAME = 120;
const LOAD_ERROR = 'No pudimos cargar el establecimiento. Intentá de nuevo.';
const SAVE_ERROR = 'No pudimos guardar los cambios. Intentá de nuevo.';

/** Required check that treats whitespace-only text as empty. */
function notBlank(control: AbstractControl<string>): ValidationErrors | null {
  return control.value.trim() === '' ? { required: true } : null;
}

/** H8.4: the admin renames their establishment and sees its basic data. */
@Component({
  selector: 'app-establishment-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './establishment-page.html',
  styleUrl: './establishment-page.scss',
})
export class EstablishmentPage implements OnInit {
  private readonly service = inject(CurrentEstablishmentService);
  private readonly auth = inject(AuthService);

  protected readonly form = inject(NonNullableFormBuilder).group({
    name: ['', [notBlank, Validators.maxLength(MAX_NAME)]],
  });

  protected readonly establishment = signal<CurrentEstablishment | null>(null);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly submitted = signal(false);
  protected readonly nameTaken = signal(false);
  protected readonly saved = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly formatDate = formatDate;

  ngOnInit(): void {
    this.form.controls.name.valueChanges.subscribe(() => {
      this.nameTaken.set(false);
      this.saved.set(false);
    });
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.service.get().subscribe({
      next: (establishment) => {
        this.establishment.set(establishment);
        this.form.controls.name.setValue(establishment.name, { emitEvent: false });
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.error.set(apiErrorMessage(err) ?? LOAD_ERROR);
      },
    });
  }

  protected nameError(): string | null {
    const control = this.form.controls.name;
    if (this.nameTaken()) {
      return ESTABLISHMENT_NAME_TAKEN_MESSAGE;
    }
    if ((!this.submitted() && !control.touched) || control.valid) {
      return null;
    }
    return control.hasError('required') ? 'Ingresá el nombre del establecimiento.' : 'Es demasiado largo.';
  }

  protected submit(): void {
    if (this.saving()) {
      return;
    }
    this.submitted.set(true);
    this.error.set(null);
    this.saved.set(false);
    if (this.form.invalid) {
      return;
    }
    this.saving.set(true);
    this.service.rename(this.form.controls.name.value.trim()).subscribe({
      next: (establishment) => {
        this.saving.set(false);
        this.establishment.set(establishment);
        this.form.controls.name.setValue(establishment.name, { emitEvent: false });
        this.saved.set(true);
        this.auth.refreshCurrentUser();
      },
      error: (err: unknown) => {
        this.saving.set(false);
        if (apiErrorCode(err) === ESTABLISHMENT_ERROR_CODES.nameTaken) {
          this.nameTaken.set(true);
        } else {
          this.error.set(apiErrorMessage(err) ?? SAVE_ERROR);
        }
      },
    });
  }
}
