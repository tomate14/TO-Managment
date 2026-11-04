import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { DiagnosisPage } from './diagnosis-page';
import { DiagnosesService } from '../diagnoses.service';
import { Diagnosis } from '../diagnoses.models';

describe('DiagnosisPage', () => {
  const surgical: Diagnosis = {
    id: 3, patient: { id: 7, firstName: 'Lucía', lastName: 'Fernández', dni: '30123456' },
    description: 'Fractura distal de radio izquierdo', createdDate: '2026-10-12',
    treatment: {
      type: 'SURGICAL', surgeryDate: '2026-10-14', stitchesRemovalDate: '2026-10-28',
      surgeryType: { id: 1, name: 'Osteosíntesis con placa', active: true }, immobilizationDate: null, requiresScarType: true,
    },
  };
  let get: ReturnType<typeof vi.fn>;

  function render() {
    TestBed.configureTestingModule({
      imports: [DiagnosisPage],
      providers: [
        provideRouter([]),
        { provide: DiagnosesService, useValue: { get } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '7', diagnosisId: '3' }) } } },
      ],
    });
    const fixture = TestBed.createComponent(DiagnosisPage);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    get = vi.fn(() => of(surgical));
  });

  it('shows the patient, the description, the badge and the surgical data', () => {
    const el = render();
    expect(get).toHaveBeenCalledWith(3);
    expect(el.textContent).toContain('Lucía Fernández · DNI 30.123.456');
    expect(el.querySelector('#diagnosis-description')?.textContent).toContain('Fractura distal de radio izquierdo');
    expect(el.querySelector('.treatment-badge--surgical')?.textContent).toContain('Quirúrgico');
    const data = el.querySelector('.diagnosis-summary__data')?.textContent ?? '';
    expect(data).toContain('12/10/2026');
    expect(data).toContain('14/10/2026');
    expect(data).toContain('28/10/2026');
    expect(data).toContain('Osteosíntesis con placa');
  });

  it('shows the conservative data', () => {
    get.mockReturnValue(of({
      ...surgical,
      treatment: { type: 'CONSERVATIVE', surgeryDate: null, stitchesRemovalDate: null, surgeryType: null,
        immobilizationDate: '2026-08-02', requiresScarType: false },
    }));
    const el = render();
    expect(el.textContent).toContain('Conservador');
    expect(el.textContent).toContain('Inmovilización');
    expect(el.textContent).not.toContain('Retiro de puntos');
  });

  it('has the follow-ups section and a way back to the patient', () => {
    const el = render();
    expect(el.querySelector('#follow-ups-heading')?.textContent).toContain('Seguimientos');
    expect(el.textContent).toContain('Todavía no hay seguimientos.');
    expect(el.querySelector('a.page-header__back')?.getAttribute('href')).toBe('/pacientes/7');
  });

  it('says so when the diagnosis does not exist', () => {
    get.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    const el = render();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Este diagnóstico no existe.');
  });
});
