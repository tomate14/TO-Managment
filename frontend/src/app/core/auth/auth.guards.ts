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

/** Admin-only sections; other roles go back to the home section. Runs after authGuard (parent route). */
export const adminGuard: CanActivateFn = () => {
  const isAdmin = inject(AuthService).currentUser()?.role === 'ADMIN';
  return isAdmin ? true : inject(Router).createUrlTree(['/pacientes']);
};

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.hasValidSession() ? inject(Router).createUrlTree(['/pacientes']) : true;
};
