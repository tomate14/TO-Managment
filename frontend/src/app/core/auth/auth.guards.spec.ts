import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { adminGuard, authGuard, establishmentGuard, guestGuard, superadminGuard } from './auth.guards';
import { UserSummary } from './auth.models';
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

  it('adminGuard lets admins through', () => {
    localStorage.clear();
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { currentUser: () => ({ id: 1, email: 'a@c.com', fullName: 'A', role: 'ADMIN' }) } },
      ],
    });
    expect(run(adminGuard)).toBe(true);
  });

  it('adminGuard sends therapists to /pacientes', () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { currentUser: () => ({ id: 2, email: 't@c.com', fullName: 'T', role: 'TERAPEUTA' }) } },
      ],
    });
    const result = run(adminGuard);
    expect(result instanceof UrlTree).toBe(true);
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/pacientes');
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

  describe('multi-tenant guards', () => {
    const norte = { id: 3, name: 'Norte', kind: 'INSTITUTION' as const };

    function withUser(user: Partial<UserSummary>) {
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          provideRouter([]),
          { provide: AuthService, useValue: { currentUser: () => ({ id: 1, email: 'a@c.com', fullName: 'A', establishment: null, ...user }) } },
        ],
      });
    }

    function url(result: unknown): string {
      return TestBed.inject(Router).serializeUrl(result as UrlTree);
    }

    it('sends a superadmin without establishment to /establecimientos', () => {
      withUser({ role: 'SUPERADMIN' });
      expect(url(run(establishmentGuard))).toBe('/establecimientos');
    });

    it('establishmentGuard lets a session with establishment through', () => {
      withUser({ role: 'ADMIN', establishment: norte });
      expect(run(establishmentGuard)).toBe(true);
    });

    it('lets the superadmin inside an establishment use admin screens', () => {
      withUser({ role: 'SUPERADMIN', establishment: norte });
      expect(run(adminGuard)).toBe(true);
    });

    it('adminGuard sends a superadmin outside an establishment to /pacientes', () => {
      withUser({ role: 'SUPERADMIN' });
      expect(url(run(adminGuard))).toBe('/pacientes');
    });

    it('superadminGuard accepts only a superadmin outside an establishment', () => {
      withUser({ role: 'SUPERADMIN' });
      expect(run(superadminGuard)).toBe(true);
      withUser({ role: 'SUPERADMIN', establishment: norte });
      expect(url(run(superadminGuard))).toBe('/pacientes');
      withUser({ role: 'ADMIN', establishment: norte });
      expect(url(run(superadminGuard))).toBe('/pacientes');
    });
  });
});
