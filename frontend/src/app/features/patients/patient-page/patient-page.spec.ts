import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { PatientPage } from './patient-page';
import { PatientsService } from '../patients.service';
import { Patient } from '../patients.models';

describe('PatientPage', () => {
  const lucia: Patient = {
    id: 7, dni: '30123456', firstName: 'Lucía', lastName: 'Fernández', birthDate: '1984-03-21', phone: '249 412-3456',
    occupation: 'Docente', healthInsurance: 'OSDE', healthInsuranceNumber: '61 234567 8 01', dominance: 'RIGHT',
  };
  let get: ReturnType<typeof vi.fn>;

  function render() {
    TestBed.configureTestingModule({
      imports: [PatientPage],
      providers: [
        provideRouter([]),
        { provide: PatientsService, useValue: { get } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '7' }) } } },
      ],
    });
    const fixture = TestBed.createComponent(PatientPage);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    get = vi.fn(() => of(lucia));
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
