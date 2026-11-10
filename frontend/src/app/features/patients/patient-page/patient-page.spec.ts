import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { BillingService } from '../../billing/billing.service';
import { PatientPage } from './patient-page';
import { PatientsService } from '../patients.service';
import { Patient } from '../patients.models';
import { DiagnosesService } from '../../diagnoses/diagnoses.service';
import { Diagnosis } from '../../diagnoses/diagnoses.models';

describe('PatientPage', () => {
  const lucia: Patient = {
    id: 7, dni: '30123456', firstName: 'Lucía', lastName: 'Fernández', birthDate: '1984-03-21', phone: '249 412-3456',
    occupation: 'Docente', healthInsurance: 'OSDE', healthInsuranceNumber: '61 234567 8 01', dominance: 'RIGHT',
  };
  let get: ReturnType<typeof vi.fn>;
  let listForPatient: ReturnType<typeof vi.fn>;

  function render() {
    TestBed.configureTestingModule({
      imports: [PatientPage],
      providers: [
        provideRouter([]),
        { provide: BillingService, useValue: { summary: () => of({ totalSessions: 0, billedSessions: 0, pendingSessions: 0, pending: [], billings: [] }) } },
        { provide: PatientsService, useValue: { get } },
        { provide: DiagnosesService, useValue: { listForPatient } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '7' }) } } },
      ],
    });
    const fixture = TestBed.createComponent(PatientPage);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    get = vi.fn(() => of(lucia));
    listForPatient = vi.fn(() => of([]));
  });

  it('lists the diagnoses newest first and links to a new one', () => {
    const patientRef = { id: 7, firstName: 'Lucía', lastName: 'Fernández', dni: '30123456' };
    const diagnoses: Diagnosis[] = [
      { id: 2, patient: patientRef, description: 'Fractura distal de radio izquierdo', createdDate: '2026-10-12',
        treatment: { type: 'SURGICAL', surgeryDate: '2026-10-14', stitchesRemovalDate: '2026-10-28',
          surgeryType: { id: 1, name: 'Osteosíntesis con placa', active: true }, hasImmobilization: null, immobilizationDate: null, requiresScarType: true }, followUpCount: 2 },
      { id: 1, patient: patientRef, description: 'Tenosinovitis de De Quervain', createdDate: '2026-08-01',
        treatment: { type: 'CONSERVATIVE', surgeryDate: null, stitchesRemovalDate: null, surgeryType: null,
          hasImmobilization: true, immobilizationDate: '2026-08-02', requiresScarType: false }, followUpCount: 0 },
    ];
    listForPatient.mockReturnValue(of(diagnoses));
    const el = render();

    expect(listForPatient).toHaveBeenCalledWith(7);
    const cards = el.querySelectorAll('app-diagnosis-card');
    expect(cards.length).toBe(2);
    expect(cards[0].textContent).toContain('Fractura distal de radio izquierdo');
    expect(el.querySelector('a.diagnoses__new')?.getAttribute('href')).toBe('/pacientes/7/diagnosticos/nuevo');
  });

  it('shows patient billing before diagnoses in the same content column', () => {
    const el = render();
    const content = el.querySelector('.patient-content')!;
    const billing = content?.querySelector('app-billing-section');
    const diagnoses = content?.querySelector('.diagnoses');
    expect(billing?.textContent).toContain('Facturación');
    expect(billing!.compareDocumentPosition(diagnoses!) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('shows the name, DNI, age and dominance', () => {
    const el = render();
    expect(get).toHaveBeenCalledWith(7);
    expect(el.querySelector('h1')?.textContent).toContain('Lucía Fernández');
    const summary = el.querySelector('.patient-hero__summary')?.textContent ?? '';
    expect(summary).toMatch(/^DNI 30\.123\.456 · \d+ años · Diestro$/);
    expect(el.querySelector('.patient-hero__avatar')?.textContent).toContain('LF');
  });

  it('lists the personal data with a dialable phone', () => {
    const el = render();
    const data = el.querySelector('.patient-data')?.textContent ?? '';
    expect(data).toContain('21/03/1984');
    expect(data).toContain('Docente');
    expect(data).toContain('OSDE');
    expect(data).toContain('61 234567 8 01');
    expect(el.querySelector('.patient-data a')?.getAttribute('href')).toBe('tel:2494123456');
  });

  it('links to the edit form and shows the diagnoses section', () => {
    const el = render();
    expect(el.querySelector('a.patient-hero__edit')?.getAttribute('href')).toBe('/pacientes/7/editar');
    expect(el.querySelector('#diagnoses-heading')?.textContent).toContain('Diagnósticos');
    expect(el.textContent).toContain('Todavía no hay diagnósticos.');
  });

  it('says so when the patient does not exist', () => {
    get.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    const el = render();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Este paciente no existe.');
  });
});
