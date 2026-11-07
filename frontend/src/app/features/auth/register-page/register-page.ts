import { Component, ElementRef, Injector, afterNextRender, inject, signal, viewChild } from '@angular/core';
import {
  AbstractControl,
  FormControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { apiErrorCode, apiErrorMessage } from '../../../core/api/api-error';
import { LoginHero } from '../login-hero/login-hero';
import { RegistrationService } from '../registration.service';

type RegisterField = 'fullName' | 'email' | 'password' | 'confirm';
type RegisterFailure = 'emailTaken' | 'tooMany' | 'server' | 'connection';

const LOGIN_URL = '/login';
const FULL_NAME_MAX = 120;
const EMAIL_MAX = 254;
const PASSWORD_MIN = 8;
const PASSWORD_MAX = 72;

const MESSAGES = {
  emailTaken: 'Ya hay una cuenta con este email.',
  tooMany: 'Demasiados intentos. Probá de nuevo más tarde.',
  mismatch: 'Las contraseñas no coinciden.',
  connection: 'No pudimos conectar con el servidor. Intentá de nuevo en unos minutos.',
  serverFallback: 'Revisá los datos e intentá de nuevo.',
};

const FIELD_MESSAGES: Record<Exclude<RegisterField, 'confirm'>, string> = {
  fullName: 'Ingresá tu nombre y apellido (hasta 120 caracteres).',
  email: 'Ingresá un email válido, como nombre@consultorio.com.',
  password: 'La contraseña debe tener entre 8 y 72 caracteres.',
};

function notBlank(control: AbstractControl<string>): ValidationErrors | null {
  return control.value.trim() === '' ? { blank: true } : null;
}

/** Email format check on the trimmed value, so a trailing space from mobile autocomplete is not a format error. */
function trimmedEmail(control: AbstractControl<string>): ValidationErrors | null {
  return Validators.email(new FormControl(control.value.trim()));
}

function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value as string;
  const confirm = group.get('confirm')?.value as string;
  return password === confirm ? null : { mismatch: true };
}

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink, LoginHero],
  templateUrl: './register-page.html',
  styleUrl: './register-page.scss',
})
export class RegisterPage {
  private readonly registration = inject(RegistrationService);
  private readonly router = inject(Router);
  private readonly injector = inject(Injector);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly errorBanner = viewChild<ElementRef<HTMLElement>>('errorBanner');

  protected readonly form = this.formBuilder.group(
    {
      fullName: ['', [Validators.required, notBlank, Validators.maxLength(FULL_NAME_MAX)]],
      email: ['', [Validators.required, trimmedEmail, Validators.maxLength(EMAIL_MAX)]],
      password: ['', [Validators.required, Validators.minLength(PASSWORD_MIN), Validators.maxLength(PASSWORD_MAX)]],
      confirm: ['', [Validators.required]],
    },
    { validators: [passwordsMatch] },
  );

  protected readonly loading = signal(false);
  protected readonly submitted = signal(false);
  protected readonly failure = signal<RegisterFailure | null>(null);
  protected readonly serverMessage = signal<string | null>(null);
  protected readonly sentTo = signal<string | null>(null);
  protected readonly showPassword = signal(false);

  protected readonly fieldMessages = FIELD_MESSAGES;
  protected readonly messages = MESSAGES;

  constructor() {
    this.form.valueChanges.subscribe(() => {
      if (!this.loading()) {
        this.failure.set(null);
        this.serverMessage.set(null);
      }
    });
  }

  protected togglePassword(): void {
    this.showPassword.update((shown) => !shown);
  }

  /** The field message to show: only after the field was touched or a submit was attempted. */
  protected showError(field: RegisterField): boolean {
    const control = this.form.controls[field];
    if (!this.submitted() && !control.touched) {
      return false;
    }
    if (field === 'confirm') {
      return control.hasError('required') || this.form.hasError('mismatch');
    }
    if (field === 'email' && this.failure() === 'emailTaken') {
      return true;
    }
    return control.invalid;
  }

  protected isEmailTaken(): boolean {
    return this.failure() === 'emailTaken';
  }

  protected goToLogin(): void {
    void this.router.navigateByUrl(LOGIN_URL);
  }

  protected submit(): void {
    if (this.loading()) {
      return;
    }
    this.submitted.set(true);
    this.failure.set(null);
    this.serverMessage.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { fullName, email, password } = this.form.getRawValue();
    const payload = { fullName: fullName.trim(), email: email.trim(), password };
    this.loading.set(true);
    this.registration.register(payload).subscribe({
      next: () => {
        this.loading.set(false);
        this.sentTo.set(payload.email);
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.fail(err);
      },
    });
  }

  private fail(err: unknown): void {
    if (apiErrorCode(err) === 'USER_EMAIL_TAKEN') {
      this.failure.set('emailTaken');
      return;
    }
    if (err instanceof HttpErrorResponse && err.status === 429) {
      this.failure.set('tooMany');
    } else if (apiErrorCode(err) === 'VALIDATION_ERROR') {
      this.serverMessage.set(apiErrorMessage(err) ?? MESSAGES.serverFallback);
      this.failure.set('server');
    } else {
      this.failure.set('connection');
    }
    afterNextRender(() => this.errorBanner()?.nativeElement.focus(), { injector: this.injector });
  }
}
