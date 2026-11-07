import { Component, ElementRef, Injector, afterNextRender, inject, signal, viewChild } from '@angular/core';
import { AbstractControl, FormControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/auth/auth.service';
import { SESSION_EXPIRED_QUERY, TRIAL_EXPIRED_QUERY } from '../../../core/auth/auth.models';
import { apiErrorCode } from '../../../core/api/api-error';
import { LoginHero } from '../login-hero/login-hero';

type LoginErrorKind = 'credentials' | 'incomplete' | 'emailFormat' | 'inactive' | 'pending' | 'trialExpired' | 'connection';

interface Banner {
  title: string;
  message: string;
  icon: 'alert' | 'clock' | 'hourglass';
  /** Amber (warning) instead of red: the account is fine, it just can't get in yet. */
  warning?: boolean;
}

const ERROR_BANNERS: Record<LoginErrorKind, Banner> = {
  credentials: { title: 'Email o contraseña incorrectos', message: 'Revisá los datos e intentá de nuevo.', icon: 'alert' },
  incomplete: { title: 'Completá tu email y contraseña', message: 'Necesitamos los dos datos para ingresar.', icon: 'alert' },
  emailFormat: { title: 'Revisá el formato del email', message: 'Tiene que ser como nombre@consultorio.com.', icon: 'alert' },
  inactive: { title: 'Tu establecimiento está inactivo', message: 'Pedile al administrador de la plataforma que lo reactive.', icon: 'alert' },
  pending: {
    title: 'Tu cuenta está pendiente de aprobación',
    message: 'Vas a poder ingresar cuando el administrador de la plataforma la apruebe.',
    icon: 'clock',
    warning: true,
  },
  trialExpired: { title: 'Tu período de prueba venció', message: 'Contactá al administrador de la plataforma.', icon: 'hourglass' },
  connection: { title: 'No pudimos conectar con el servidor', message: 'Intentá de nuevo en unos minutos.', icon: 'alert' },
};

const HOME_URL = '/pacientes';

/** Email format check on the trimmed value, so a trailing space from mobile autocomplete is not a format error. */
function trimmedEmail(control: AbstractControl<string>): ValidationErrors | null {
  return Validators.email(new FormControl(control.value.trim()));
}

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink, LoginHero],
  templateUrl: './login-page.html',
  styleUrl: './login-page.scss',
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly injector = inject(Injector);
  private readonly errorBanner = viewChild<ElementRef<HTMLElement>>('errorBanner');

  protected readonly form = this.formBuilder.group({
    email: ['', [trimmedEmail]],
    password: ['', [Validators.required]],
  });

  protected readonly loading = signal(false);
  protected readonly showPassword = signal(false);
  private readonly route = inject(ActivatedRoute);
  protected readonly error = signal<LoginErrorKind | null>(
    this.route.snapshot.queryParamMap.get('prueba') === TRIAL_EXPIRED_QUERY.prueba ? 'trialExpired' : null,
  );
  protected readonly sessionExpired = signal(
    this.route.snapshot.queryParamMap.get('sesion') === SESSION_EXPIRED_QUERY.sesion,
  );

  constructor() {
    this.form.valueChanges.subscribe(() => {
      if (!this.loading()) {
        this.error.set(null);
      }
    });
  }

  protected banner(kind: LoginErrorKind): Banner {
    return ERROR_BANNERS[kind];
  }

  protected isInvalid(field: 'email' | 'password'): boolean {
    const kind = this.error();
    if (kind === 'credentials') {
      return true;
    }
    if (kind === 'incomplete') {
      return this.isBlank(field);
    }
    return kind === 'emailFormat' && field === 'email';
  }

  protected togglePassword(): void {
    this.showPassword.update((shown) => !shown);
  }

  protected submit(): void {
    if (this.loading()) {
      return;
    }
    this.sessionExpired.set(false);
    if (this.isBlank('email') || this.isBlank('password')) {
      this.fail('incomplete');
      return;
    }
    if (this.form.controls.email.hasError('email')) {
      this.fail('emailFormat');
      return;
    }
    this.loading.set(true);
    const { email, password } = this.form.getRawValue();
    this.auth.login(email.trim(), password).subscribe({
      next: () => {
        this.loading.set(false);
        void this.router.navigateByUrl(HOME_URL);
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.fail(this.classify(err));
      },
    });
  }

  /** A field is missing when it is empty; the email also when it is only whitespace. */
  private isBlank(field: 'email' | 'password'): boolean {
    const value = this.form.controls[field].value;
    return (field === 'email' ? value.trim() : value) === '';
  }

  private classify(err: unknown): LoginErrorKind {
    if (err instanceof HttpErrorResponse && err.status === 401) {
      return 'credentials';
    }
    if (err instanceof HttpErrorResponse && err.status === 400 && apiErrorCode(err) === 'VALIDATION_ERROR') {
      return 'emailFormat';
    }
    if (apiErrorCode(err) === 'ESTABLISHMENT_INACTIVE') {
      return 'inactive';
    }
    if (apiErrorCode(err) === 'ESTABLISHMENT_PENDING_APPROVAL') {
      return 'pending';
    }
    if (apiErrorCode(err) === 'TRIAL_EXPIRED') {
      return 'trialExpired';
    }
    return 'connection';
  }

  private fail(kind: LoginErrorKind): void {
    this.error.set(kind);
    afterNextRender(() => this.errorBanner()?.nativeElement.focus(), { injector: this.injector });
  }
}
