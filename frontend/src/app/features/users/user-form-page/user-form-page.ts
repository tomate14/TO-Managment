import { Component, OnInit, inject, signal } from '@angular/core';
import { AbstractControl, FormControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { Role } from '../../../core/auth/auth.models';
import { apiErrorCode, apiErrorMessage } from '../../../core/api/api-error';
import { USER_ERROR_CODES } from '../users.models';
import { UsersService } from '../users.service';

type FieldName = 'fullName' | 'email' | 'password';

type AssignableRole = Exclude<Role, 'SUPERADMIN'>;

interface RoleOption {
  value: AssignableRole;
  label: string;
  hint: string;
}

const ROLE_OPTIONS: readonly RoleOption[] = [
  { value: 'TERAPEUTA', label: 'Terapeuta', hint: 'Pacientes, diagnósticos, seguimientos y plantillas.' },
  { value: 'ADMIN', label: 'Admin', hint: 'Todo lo anterior, más usuarios y catálogos.' },
];

const MIN_PASSWORD = 8;
const MAX_PASSWORD = 72;
const MAX_NAME = 120;
const MAX_EMAIL = 254;
const USERS_URL = '/usuarios';
const SAVE_ERROR = 'No pudimos guardar los cambios. Intentá de nuevo.';
const LOAD_ERROR = 'No pudimos cargar el usuario.';

/** Email format check on the trimmed value (mobile autocomplete adds trailing spaces). */
function trimmedEmail(control: AbstractControl<string>): ValidationErrors | null {
  return Validators.email(new FormControl(control.value.trim()));
}

/** Required check that treats whitespace-only text as empty. */
function notBlank(control: AbstractControl<string>): ValidationErrors | null {
  return control.value.trim() === '' ? { required: true } : null;
}

/** H1.4 new user (`/usuarios/nuevo`) and edit user (`/usuarios/:id`) form. */
@Component({
  selector: 'app-user-form-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './user-form-page.html',
  styleUrl: './user-form-page.scss',
})
export class UserFormPage implements OnInit {
  private readonly usersService = inject(UsersService);
  private readonly router = inject(Router);
  private readonly myId = inject(AuthService).currentUser()?.id ?? null;
  private readonly idParam = inject(ActivatedRoute).snapshot.paramMap.get('id');

  protected readonly userId = this.idParam === null ? null : Number(this.idParam);
  protected readonly isEdit = this.userId !== null;
  protected readonly isSelf = this.isEdit && this.userId === this.myId;
  protected readonly roleOptions = ROLE_OPTIONS;

  protected readonly form = inject(NonNullableFormBuilder).group({
    fullName: ['', [notBlank, Validators.maxLength(MAX_NAME)]],
    email: ['', [notBlank, trimmedEmail, Validators.maxLength(MAX_EMAIL)]],
    password: ['', [Validators.required, Validators.minLength(MIN_PASSWORD), Validators.maxLength(MAX_PASSWORD)]],
    role: ['TERAPEUTA' as Role],
  });

  protected readonly submitted = signal(false);
  protected readonly saving = signal(false);
  protected readonly loading = signal(this.isEdit);
  protected readonly emailTaken = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    if (this.isEdit) {
      this.form.controls.email.disable();
      this.form.controls.password.disable();
    }
    if (this.isSelf) {
      this.form.controls.role.disable();
    }
    this.form.controls.email.valueChanges.subscribe(() => this.emailTaken.set(false));
  }

  ngOnInit(): void {
    if (this.userId === null) {
      return;
    }
    this.usersService.get(this.userId).subscribe({
      next: (user) => {
        this.form.patchValue({ fullName: user.fullName, email: user.email, role: user.role });
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set(LOAD_ERROR);
      },
    });
  }

  /** Message shown under a field, or null when it is valid or the user has not tried to save yet. */
  protected fieldError(field: FieldName): string | null {
    const control = this.form.controls[field];
    if (field === 'email' && this.emailTaken()) {
      return 'Ya hay un usuario con este email.';
    }
    if (control.disabled || (!this.submitted() && !control.touched) || control.valid) {
      return null;
    }
    if (control.hasError('required')) {
      return { fullName: 'Ingresá el nombre.', email: 'Ingresá el email.', password: 'Ingresá la contraseña inicial.' }[field];
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
    if (this.saving() || this.loading()) {
      return;
    }
    this.submitted.set(true);
    this.error.set(null);
    if (this.form.invalid) {
      return;
    }
    this.saving.set(true);
    const { fullName, email, password, role } = this.form.getRawValue();
    const request = this.userId === null
      ? this.usersService.create({ email: email.trim(), fullName: fullName.trim(), password, role })
      : this.usersService.update(this.userId, { fullName: fullName.trim(), role });
    request.subscribe({
      next: () => {
        this.saving.set(false);
        void this.router.navigateByUrl(USERS_URL);
      },
      error: (err: unknown) => {
        this.saving.set(false);
        if (apiErrorCode(err) === USER_ERROR_CODES.emailTaken) {
          this.emailTaken.set(true);
          return;
        }
        this.error.set(apiErrorMessage(err) ?? SAVE_ERROR);
      },
    });
  }
}
