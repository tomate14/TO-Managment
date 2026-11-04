import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PatientsService } from './patients.service';
import { PatientData } from './patients.models';

describe('PatientsService', () => {
  let service: PatientsService;
  let http: HttpTestingController;
  const data: PatientData = {
    firstName: 'Lucía', lastName: 'Fernández', birthDate: '1984-03-21', phone: '249 412-3456', occupation: 'Docente',
    healthInsurance: 'OSDE', healthInsuranceNumber: '61 234567 8 01', dominance: 'RIGHT',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(PatientsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('searches with page and size, and only sends q when there is text', () => {
    service.search('  ', 0).subscribe();
    const all = http.expectOne((req) => req.url === '/api/patients');
    expect(all.request.params.get('page')).toBe('0');
    expect(all.request.params.get('size')).toBe('20');
    expect(all.request.params.has('q')).toBe(false);

    service.search(' lucía ', 2).subscribe();
    const filtered = http.expectOne((req) => req.url === '/api/patients');
    expect(filtered.request.params.get('q')).toBe('lucía');
    expect(filtered.request.params.get('page')).toBe('2');
  });

  it('gets, creates and updates patients', () => {
    service.get(5).subscribe();
    expect(http.expectOne('/api/patients/5').request.method).toBe('GET');

    service.create({ ...data, dni: '30123456' }).subscribe();
    const create = http.expectOne('/api/patients');
    expect(create.request.method).toBe('POST');
    expect(create.request.body.dni).toBe('30123456');

    service.update(5, data).subscribe();
    const update = http.expectOne('/api/patients/5');
    expect(update.request.method).toBe('PUT');
    expect(update.request.body).toEqual(data);
  });
});
