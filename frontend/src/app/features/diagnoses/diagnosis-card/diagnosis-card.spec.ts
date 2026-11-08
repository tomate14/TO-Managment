import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { DiagnosisCard } from './diagnosis-card';
import { Diagnosis } from '../diagnoses.models';

describe('DiagnosisCard', () => {
  const patient = { id: 7, firstName: 'Lucía', lastName: 'Fernández', dni: '30123456' };
  const surgical: Diagnosis = {
    id: 3, patient, description: 'Fractura distal de radio izquierdo', createdDate: '2026-10-12',
    treatment: {
      type: 'SURGICAL', surgeryDate: '2026-10-14', stitchesRemovalDate: '2026-10-28',
      surgeryType: { id: 1, name: 'Osteosíntesis con placa', active: true }, hasImmobilization: null, immobilizationDate: null, requiresScarType: true,
    },
    followUpCount: 3,
  };
  const conservative: Diagnosis = {
    id: 4, patient, description: 'Tenosinovitis de De Quervain', createdDate: '2026-08-01',
    treatment: {
      type: 'CONSERVATIVE', surgeryDate: null, stitchesRemovalDate: null, surgeryType: null,
      hasImmobilization: true, immobilizationDate: '2026-08-02', requiresScarType: false,
    },
    followUpCount: 1,
  };

  function render(diagnosis: Diagnosis): HTMLElement {
    TestBed.configureTestingModule({ imports: [DiagnosisCard], providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(DiagnosisCard);
    fixture.componentRef.setInput('diagnosis', diagnosis);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('shows "Sin inmovilización" for a conservative diagnosis without immobilization', () => {
    const el = render({
      ...conservative,
      treatment: { ...conservative.treatment, hasImmobilization: false, immobilizationDate: null },
    });
    expect(el.textContent).toContain('Inmovilización');
    expect(el.textContent).toContain('Sin inmovilización');
  });

  it('shows a surgical diagnosis with surgery date and type', () => {
    const el = render(surgical);
    expect(el.textContent).toContain('Fractura distal de radio izquierdo');
    expect(el.querySelector('.treatment-badge--surgical')?.textContent).toContain('Quirúrgico');
    expect(el.textContent).toContain('14/10/2026');
    expect(el.textContent).toContain('Osteosíntesis con placa');
    expect(el.textContent).toContain('Creado el 12/10/2026 · 3 seguimientos');
    expect(el.querySelector('a')?.getAttribute('href')).toBe('/pacientes/7/diagnosticos/3');
  });

  it('shows a conservative diagnosis with the immobilization date', () => {
    const el = render(conservative);
    expect(el.querySelector('.treatment-badge')?.textContent).toContain('Conservador');
    expect(el.querySelector('.treatment-badge--surgical')).toBeNull();
    expect(el.textContent).toContain('Inmovilización');
    expect(el.textContent).toContain('02/08/2026');
  });
});
