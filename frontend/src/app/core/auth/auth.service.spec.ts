import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { LoginResponse } from './auth.models';

function jwtWithExp(expSeconds: number): string {
  return `h.${btoa(JSON.stringify({ exp: expSeconds })).replace(/=+$/, '')}.s`;
}

const futureToken = jwtWithExp(Date.now() / 1000 + 3600);
const response: LoginResponse = {
  token: futureToken,
  expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
  user: { id: 1, email: 'ana@consultorio.com', fullName: 'Ana Pérez', role: 'TERAPEUTA' },
};

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('login posts credentials and stores the session', () => {
    let user: unknown;
    service.login('ana@consultorio.com', 'secreta').subscribe((u) => (user = u));

    const req = http.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'ana@consultorio.com', password: 'secreta' });
    req.flush(response);

    expect(user).toEqual(response.user);
    expect(service.token()).toBe(futureToken);
    expect(service.currentUser()).toEqual(response.user);
    expect(service.hasValidSession()).toBe(true);
  });

  it('restores a stored session on a new instance', () => {
    service.login('ana@consultorio.com', 'secreta').subscribe();
    http.expectOne('/api/auth/login').flush(response);

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    const restored = TestBed.inject(AuthService);

    expect(restored.currentUser()).toEqual(response.user);
    expect(restored.hasValidSession()).toBe(true);
  });

  it('never stores the password', () => {
    service.login('ana@consultorio.com', 'secreta').subscribe();
    http.expectOne('/api/auth/login').flush(response);

    expect(JSON.stringify({ ...localStorage })).not.toContain('secreta');
  });

  it('endSession(expired) clears the session and navigates to login with the expired notice', async () => {
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    service.login('ana@consultorio.com', 'secreta').subscribe();
    http.expectOne('/api/auth/login').flush(response);

    service.endSession('expired');

    expect(service.token()).toBeNull();
    expect(service.currentUser()).toBeNull();
    expect(navigate).toHaveBeenCalledWith(['/login'], { queryParams: { sesion: 'vencida' } });
  });

  it('logout clears the stored session and navigates to login without notice', () => {
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    service.login('ana@consultorio.com', 'secreta').subscribe();
    http.expectOne('/api/auth/login').flush(response);

    service.logout();

    expect(service.token()).toBeNull();
    expect(service.currentUser()).toBeNull();
    expect(localStorage.length).toBe(0);
    expect(navigate).toHaveBeenCalledWith(['/login'], {});
  });

  it('refreshCurrentUser updates the user from /api/auth/me', () => {
    service.login('ana@consultorio.com', 'secreta').subscribe();
    http.expectOne('/api/auth/login').flush(response);

    service.refreshCurrentUser();
    http.expectOne('/api/auth/me').flush({ ...response.user, fullName: 'Ana P. Gómez' });

    expect(service.currentUser()?.fullName).toBe('Ana P. Gómez');
  });
});
