import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { FollowUpFormPage } from './follow-up-form-page';
import { FollowUpsService } from '../follow-ups.service';
import { DiagnosesService } from '../../diagnoses/diagnoses.service';
import { CatalogsService } from '../../catalogs/catalogs.service';
import { TemplatesService } from '../../templates/templates.service';
import { Diagnosis } from '../../diagnoses/diagnoses.models';
import { CatalogSlug } from '../../catalogs/catalogs.models';

describe('FollowUpFormPage', () => {
  const surgical: Diagnosis = {
    id: 3, patient: { id: 7, firstName: 'Lucía', lastName: 'Fernández', dni: '30123456' },
    description: 'Fractura distal de radio', createdDate: '2026-10-12', followUpCount: 0,
    treatment: {
      type: 'SURGICAL', surgeryDate: '2026-10-14', stitchesRemovalDate: '2026-10-28',
      surgeryType: { id: 1, name: 'Osteosíntesis con placa', active: true }, immobilizationDate: null, requiresScarType: true,
    },
  };
  const conservative: Diagnosis = {
    ...surgical,
    treatment: { type: 'CONSERVATIVE', surgeryDate: null, stitchesRemovalDate: null, surgeryType: null,
      immobilizationDate: '2026-10-01', requiresScarType: false },
  };
  let create: ReturnType<typeof vi.fn>;

  function setup(diagnosis: Diagnosis) {
    create = vi.fn(() => of({ id: 9 }));
    const catalogs: Record<CatalogSlug, { id: number; name: string; active: boolean }[]> = {
      'scar-types': [{ id: 1, name: 'Normotrófica', active: true }],
      'body-segments': [{ id: 3, name: 'Muñeca', active: true }, { id: 4, name: 'Mano', active: true }],
      'surgery-types': [],
    };
    TestBed.configureTestingModule({
      imports: [FollowUpFormPage],
      providers: [
        provideRouter([]),
        { provide: FollowUpsService, useValue: { create } },
        { provide: DiagnosesService, useValue: { get: vi.fn(() => of(diagnosis)) } },
        { provide: CatalogsService, useValue: { list: vi.fn((slug: CatalogSlug) => of(catalogs[slug])) } },
        { provide: TemplatesService, useValue: { listForSegment: vi.fn(() => of([])), get: vi.fn() } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '7', diagnosisId: '3' }) } } },
      ],
    });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(FollowUpFormPage);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const type = (selector: string, value: string) => {
      const input = el.querySelector<HTMLInputElement | HTMLSelectElement>(selector)!;
      input.value = value;
      input.dispatchEvent(new Event(input instanceof HTMLSelectElement ? 'change' : 'input'));
      fixture.detectChanges();
    };
    const submit = () => {
      el.querySelector('form')!.dispatchEvent(new Event('submit'));
      fixture.detectChanges();
    };
    return { el, type, submit, navigate };
  }

  function fillSegment(type: (selector: string, value: string) => void) {
    type('#evaluation-0-segment', '0: 3');
    type('#evaluation-0-rom', '55');
    type('#evaluation-0-strength', '14,5');
  }

  it('asks for the scar type only for surgical treatments', () => {
    expect(setup(surgical).el.querySelector('#scar-type')).not.toBeNull();
    TestBed.resetTestingModule();
    expect(setup(conservative).el.querySelector('#scar-type')).toBeNull();
  });

  it('shows the problems instead of saving an incomplete follow-up', () => {
    const { el, submit } = setup(surgical);
    submit();
    expect(create).not.toHaveBeenCalled();
    expect(el.textContent).toContain('Elegí el tipo de cicatriz.');
    expect(el.textContent).toContain('Ingresá el ROM.');
  });

  it('saves the whole follow-up and goes back to the diagnosis', () => {
    const { type, submit, navigate } = setup(conservative);
    fillSegment(type);
    submit();
    expect(create).toHaveBeenCalledWith(3, expect.objectContaining({
      scarTypeId: null,
      evaluations: [{ segmentId: 3, edema: null, pain: 0, rom: 55, strength: 14.5, templateId: null, answers: [] }],
    }));
    expect(navigate).toHaveBeenCalledWith(['/pacientes', 7, 'diagnosticos', 3], { replaceUrl: true });
  });

  it('shows out-of-range errors under their field', () => {
    const { el, type, submit } = setup(conservative);
    create.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 422,
      error: { code: 'FOLLOW_UP_OUT_OF_RANGE', message: 'Hay valores fuera del rango permitido.',
        fieldErrors: [{ field: 'evaluations[0].rom', message: 'ROM fuera del rango permitido.' }] },
    })));
    fillSegment(type);
    submit();
    expect(el.querySelector('#evaluation-0-rom')?.getAttribute('aria-invalid')).toBe('true');
    expect(el.textContent).toContain('ROM fuera del rango permitido.');
  });
});
