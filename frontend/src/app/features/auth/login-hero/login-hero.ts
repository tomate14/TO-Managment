import { Component, input } from '@angular/core';

export type LoginHeroVariant = 'login' | 'register';

/**
 * Presentational brand header (mobile) / aside (desktop) of the auth screens.
 * `register` is brand-only on mobile and shows the sign-up copy on desktop.
 */
@Component({
  selector: 'app-login-hero',
  templateUrl: './login-hero.html',
  styleUrl: './login-hero.scss',
})
export class LoginHero {
  readonly variant = input<LoginHeroVariant>('login');
}
