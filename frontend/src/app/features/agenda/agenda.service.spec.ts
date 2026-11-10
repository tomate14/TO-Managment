import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AgendaService } from './agenda.service';

describe('AgendaService', () => {
  let service: AgendaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AgendaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists appointments filtered by therapists', () => {
    service.list('2026-11-02', '2026-11-08', [1, 2]).subscribe();
    const request = http.expectOne('/api/appointments?from=2026-11-02&to=2026-11-08&therapistIds=1,2');
    expect(request.request.method).toBe('GET');
  });

  it('omits therapistIds when empty', () => {
    service.list('2026-11-02', '2026-11-08').subscribe();
    http.expectOne('/api/appointments?from=2026-11-02&to=2026-11-08');
  });

  it('queries availability omitting absent optional params', () => {
    service.availability({ therapistId: 1, date: '2026-11-05', durationMinutes: 45 }).subscribe();
    http.expectOne('/api/appointments/availability?therapistId=1&date=2026-11-05&durationMinutes=45');
  });

  it('gets one appointment', () => {
    service.get(3).subscribe();
    expect(http.expectOne('/api/appointments/3').request.method).toBe('GET');
  });

  it('cancels an appointment', () => {
    service.cancel(3).subscribe();
    expect(http.expectOne('/api/appointments/3').request.method).toBe('DELETE');
  });

  it('confirms an appointment with an empty POST body', () => {
    service.confirm(3).subscribe();
    const request = http.expectOne('/api/appointments/3/confirm');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({});
  });
});
