import { Component, ElementRef, Injector, afterNextRender, computed, inject, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { apiErrorCode } from '../../../core/api/api-error';

type Outcome = 'saved' | 'wrongCurrent' | 'incomplete' | 'tooShort' | 'failed';

interface Banner {
  kind: 'error' | 'info';
  title: string;
  message: string;
}

const BANNERS: Record<Outcome, Banner> = {
  saved: { kind: 'info', title: 'Tu contraseña se actualizó', message: 'La próxima vez ingresá con la nueva.' },
  wrongCurrent: { kind: 'error', title: 'La contraseña actual no es correcta', message: 'No se guardó ningún cambio.' },
  incomplete: { kind: 'error', title: 'Completá las dos contraseñas', message: 'Necesitamos la actual y la nueva.' },
  tooShort: { kind: 'error', title: 'La nueva contraseña es muy corta', message: 'Tiene que tener al menos 8 caracteres.' },
  failed: { kind: 'error', title: 'No pudimos guardar la contraseña', message: 'Intentá de nuevo en unos minutos.' },
};

const MIN_LENGTH = 8;
const MAX_LENGTH = 72;
const STRENGTH_BARS = 4;
const CHARS_PER_BAR = 3;
const CURRENT_PASSWORD_INCORRECT = 'CURRENT_PASSWORD_INCORRECT';

/** H1.5: the signed-in user changes her own password. */
@Component({
  selector: 'app-change-password-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './change-password-page.html',
  styleUrl: './change-password-page.scss',
})
export class ChangePasswordPage {
  private readonly auth = inject(AuthService);
  private readonly injector = inject(Injector);
  private readonly bannerRef = viewChild<ElementRef<HTMLElement>>('banner');

  protected readonly form = inject(NonNullableFormBuilder).group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(MIN_LENGTH), Validators.maxLength(MAX_LENGTH)]],
  });

  protected readonly saving = signal(false);
  protected readonly outcome = signal<Outcome | null>(null);
  protected readonly minLength = MIN_LENGTH;
  protected readonly bars = Array.from({ length: STRENGTH_BARS }, (_, index) => index + 1);

  private readonly newPassword = toSignal(this.form.controls.newPassword.valueChanges, { initialValue: '' });
  protected readonly newLength = computed(() => this.newPassword().length);
  protected readonly longEnough = computed(() => this.newLength() >= MIN_LENGTH);
  /** Filled bars: one every 3 characters, up to 4 (same rule as the mockup). */
  protected readonly level = computed(() => Math.min(STRENGTH_BARS, Math.ceil(this.newLength() / CHARS_PER_BAR)));

  protected bannerFor(outcome: Outcome): Banner {
    return BANNERS[outcome];
  }

  protected currentInvalid(): boolean {
    const outcome = this.outcome();
    return outcome === 'wrongCurrent' || (outcome === 'incomplete' && this.form.controls.currentPassword.invalid);
  }

  protected newInvalid(): boolean {
    const outcome = this.outcome();
    return (outcome === 'incomplete' || outcome === 'tooShort') && this.form.controls.newPassword.invalid;
  }

  protected submit(): void {
    if (this.saving()) {
      return;
    }
    const { currentPassword, newPassword } = this.form.getRawValue();
    if (currentPassword === '' || newPassword === '') {
      this.show('incomplete');
      return;
    }
    if (this.form.controls.newPassword.invalid) {
      this.show('tooShort');
      return;
    }
    this.saving.set(true);
    this.auth.changePassword(currentPassword, newPassword).subscribe({
      next: () => {
        this.saving.set(false);
        this.form.reset();
        this.show('saved');
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.show(apiErrorCode(err) === CURRENT_PASSWORD_INCORRECT ? 'wrongCurrent' : 'failed');
      },
    });
  }

  private show(outcome: Outcome): void {
    this.outcome.set(outcome);
    afterNextRender(() => this.bannerRef()?.nativeElement.focus(), { injector: this.injector });
  }
}
