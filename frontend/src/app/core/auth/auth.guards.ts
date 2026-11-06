import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { SESSION_EXPIRED_QUERY, isAdminInEstablishment } from './auth.models';

const HOME_URL = '/pacientes';
const ESTABLISHMENTS_URL = '/establecimientos';

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

/** Admin-only sections (ADMIN, or SUPERADMIN inside an establishment); others go back home. Runs after authGuard. */
export const adminGuard: CanActivateFn = () => {
  const allowed = isAdminInEstablishment(inject(AuthService).currentUser());
  return allowed ? true : inject(Router).createUrlTree([HOME_URL]);
};

/** Superadmin outside any establishment (the establishments list); everyone else goes home. */
export const superadminGuard: CanActivateFn = () => {
  const user = inject(AuthService).currentUser();
  return user?.role === 'SUPERADMIN' && !user.establishment ? true : inject(Router).createUrlTree([HOME_URL]);
};

/**
 * Data screens need an establishment in the session; a superadmin without one picks it first.
 * Other roles always carry one (the backend enforces it), so they are not redirected (avoids a /pacientes loop).
 */
export const establishmentGuard: CanActivateFn = () => {
  const user = inject(AuthService).currentUser();
  if (user?.establishment || user?.role !== 'SUPERADMIN') {
    return true;
  }
  return inject(Router).createUrlTree([ESTABLISHMENTS_URL]);
};

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.hasValidSession() ? inject(Router).createUrlTree([HOME_URL]) : true;
};
