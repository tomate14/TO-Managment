import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DiagnosesService } from './diagnoses.service';
import { treatmentLabel } from './diagnoses.models';

describe('DiagnosesService', () => {
  let service: DiagnosesService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(DiagnosesService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists and creates under the patient and reads by id', () => {
    service.listForPatient(7).subscribe();
    expect(http.expectOne('/api/patients/7/diagnoses').request.method).toBe('GET');

    const payload = {
      description: 'Fractura', treatment: {
        type: 'CONSERVATIVE' as const, surgeryDate: null, stitchesRemovalDate: null, surgeryTypeId: null, immobilizationDate: '2026-08-02',
      },
    };
    service.create(7, payload).subscribe();
    const create = http.expectOne('/api/patients/7/diagnoses');
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual(payload);

    service.get(3).subscribe();
    expect(http.expectOne('/api/diagnoses/3').request.method).toBe('GET');
  });

  it('labels treatments as in the mockups', () => {
    expect(treatmentLabel('SURGICAL')).toBe('Quirúrgico');
    expect(treatmentLabel('CONSERVATIVE')).toBe('Conservador');
  });
});
