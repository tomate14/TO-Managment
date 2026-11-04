import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { authGuard, guestGuard } from './auth.guards';
import { AuthService } from './auth.service';

function run(guard: typeof authGuard) {
  return TestBed.runInInjectionContext(() => guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot));
}

describe('auth guards', () => {
  let auth: AuthService;
  let router: Router;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideRouter([])] });
    auth = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
  });

  it('authGuard lets a valid session through', () => {
    vi.spyOn(auth, 'hasValidSession').mockReturnValue(true);
    expect(run(authGuard)).toBe(true);
  });

  it('authGuard without a session redirects to /login', () => {
    vi.spyOn(auth, 'hasValidSession').mockReturnValue(false);
    vi.spyOn(auth, 'hasExpiredSession').mockReturnValue(false);
    expect(router.serializeUrl(run(authGuard) as UrlTree)).toBe('/login');
  });

  it('expiredTokenRedirectsToLoginWithNotice', () => {
    vi.spyOn(auth, 'hasValidSession').mockReturnValue(false);
    vi.spyOn(auth, 'hasExpiredSession').mockReturnValue(true);
    const clear = vi.spyOn(auth, 'clearSession');
    expect(router.serializeUrl(run(authGuard) as UrlTree)).toBe('/login?sesion=vencida');
    expect(clear).toHaveBeenCalled();
  });

  it('guestGuard sends an authenticated user to /pacientes', () => {
    vi.spyOn(auth, 'hasValidSession').mockReturnValue(true);
    expect(router.serializeUrl(run(guestGuard) as UrlTree)).toBe('/pacientes');
  });

  it('guestGuard lets an anonymous user see the login', () => {
    vi.spyOn(auth, 'hasValidSession').mockReturnValue(false);
    expect(run(guestGuard)).toBe(true);
  });
});
