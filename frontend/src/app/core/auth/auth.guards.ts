import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { SESSION_EXPIRED_QUERY } from './auth.models';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.hasValidSession()) {
    return true;
  }
  if (auth.hasExpiredSession()) {
    auth.clearSession();
    return router.createUrlTree(['/login'], { queryParams: SESSION_EXPIRED_QUERY });
  }
  return router.createUrlTree(['/login']);
};

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.hasValidSession() ? inject(Router).createUrlTree(['/pacientes']) : true;
};
