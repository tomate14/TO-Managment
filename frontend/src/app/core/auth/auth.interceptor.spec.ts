import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => backend.verify());

  it('adds the bearer token to API requests', () => {
    vi.spyOn(auth, 'token').mockReturnValue('abc');
    http.get('/api/patients').subscribe();
    expect(backend.expectOne('/api/patients').request.headers.get('Authorization')).toBe('Bearer abc');
  });

  it('does not send the token to the login endpoint', () => {
    vi.spyOn(auth, 'token').mockReturnValue('abc');
    http.post('/api/auth/login', {}).subscribe({ error: () => undefined });
    expect(backend.expectOne('/api/auth/login').request.headers.has('Authorization')).toBe(false);
  });

  it('does not send the token to the register endpoint', () => {
    vi.spyOn(auth, 'token').mockReturnValue('abc');
    http.post('/api/auth/register', {}).subscribe({ error: () => undefined });
    expect(backend.expectOne('/api/auth/register').request.headers.has('Authorization')).toBe(false);
  });

  it('does not send the token to non-API URLs', () => {
    vi.spyOn(auth, 'token').mockReturnValue('abc');
    http.get('https://fonts.googleapis.com/x').subscribe();
    expect(backend.expectOne('https://fonts.googleapis.com/x').request.headers.has('Authorization')).toBe(false);
  });

  it('expiredTokenResponseEndsSessionWithExpiredNotice', () => {
    vi.spyOn(auth, 'token').mockReturnValue('abc');
    const end = vi.spyOn(auth, 'endSession').mockImplementation(() => undefined);
    http.get('/api/auth/me').subscribe({ error: () => undefined });
    backend.expectOne('/api/auth/me').flush({ code: 'TOKEN_EXPIRED', message: 'x' }, { status: 401, statusText: 'Unauthorized' });
    expect(end).toHaveBeenCalledWith('expired');
  });

  it('other 401 with a token ends the session as invalid', () => {
    vi.spyOn(auth, 'token').mockReturnValue('abc');
    const end = vi.spyOn(auth, 'endSession').mockImplementation(() => undefined);
    http.get('/api/auth/me').subscribe({ error: () => undefined });
    backend.expectOne('/api/auth/me').flush({ code: 'AUTHENTICATION_REQUIRED', message: 'x' }, { status: 401, statusText: 'Unauthorized' });
    expect(end).toHaveBeenCalledWith('invalid');
  });

  it('a failed login (401) does not end any session', () => {
    vi.spyOn(auth, 'token').mockReturnValue(null);
    const end = vi.spyOn(auth, 'endSession');
    http.post('/api/auth/login', {}).subscribe({ error: () => undefined });
    backend.expectOne('/api/auth/login').flush({ code: 'AUTHENTICATION_FAILED', message: 'x' }, { status: 401, statusText: 'Unauthorized' });
    expect(end).not.toHaveBeenCalled();
  });
});
