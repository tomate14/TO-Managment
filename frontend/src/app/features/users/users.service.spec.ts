import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { UsersService } from './users.service';

describe('UsersService', () => {
  let service: UsersService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(UsersService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists users', () => {
    service.list().subscribe();
    expect(http.expectOne('/api/users').request.method).toBe('GET');
  });

  it('gets one user', () => {
    service.get(7).subscribe();
    expect(http.expectOne('/api/users/7').request.method).toBe('GET');
  });

  it('creates a user', () => {
    const payload = { email: 'c@c.com', fullName: 'Carla', password: 'clave-inicial', role: 'TERAPEUTA' as const };
    service.create(payload).subscribe();
    const req = http.expectOne('/api/users');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
  });

  it('updates name and role', () => {
    service.update(7, { fullName: 'Carla', role: 'ADMIN' }).subscribe();
    const req = http.expectOne('/api/users/7');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ fullName: 'Carla', role: 'ADMIN' });
  });

  it('changes the active flag', () => {
    service.setActive(7, false).subscribe();
    const req = http.expectOne('/api/users/7/active');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ active: false });
  });
});
