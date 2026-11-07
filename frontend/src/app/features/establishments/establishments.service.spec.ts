import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { EstablishmentsService } from './establishments.service';

describe('EstablishmentsService', () => {
  let service: EstablishmentsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(EstablishmentsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists establishments', () => {
    service.list().subscribe();
    expect(http.expectOne('/api/establishments').request.method).toBe('GET');
  });

  it('creates an establishment with its first admin', () => {
    const payload = { name: 'Kinesio Centro', admin: { fullName: 'Sofía Núñez', email: 's@k.com', password: 'clave-inicial' } };
    service.create(payload).subscribe();
    const req = http.expectOne('/api/establishments');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
  });

  it('renames an establishment', () => {
    service.rename(4, 'Nuevo nombre').subscribe();
    const req = http.expectOne('/api/establishments/4');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ name: 'Nuevo nombre' });
  });

  it('changes the active flag', () => {
    service.setActive(4, false).subscribe();
    const req = http.expectOne('/api/establishments/4/active');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ active: false });
  });

  it('approves a practice', () => {
    service.approve(4).subscribe();
    const req = http.expectOne('/api/establishments/4/approval');
    expect(req.request.method).toBe('POST');
  });

  it('changes the trial end', () => {
    service.changeTrialEnd(4, '2026-11-30').subscribe();
    const req = http.expectOne('/api/establishments/4/trial');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ trialEndsOn: '2026-11-30' });
  });

  it('removes the trial end', () => {
    service.changeTrialEnd(4, null).subscribe();
    const req = http.expectOne('/api/establishments/4/trial');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ trialEndsOn: null });
  });

  it('keeps the pending count', () => {
    expect(service.pendingCount()).toBe(0);
    service.setPendingCount(3);
    expect(service.pendingCount()).toBe(3);
  });
});
