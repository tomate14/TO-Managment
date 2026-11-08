import { vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { DiagnosisPage } from './diagnosis-page';
import { DiagnosesService } from '../diagnoses.service';
import { Diagnosis } from '../diagnoses.models';
import { FollowUp } from '../../follow-ups/follow-ups.models';
import { FollowUpsService } from '../../follow-ups/follow-ups.service';
import { BillingService } from '../../billing/billing.service';
import { BillingSummary } from '../../billing/billing.models';

describe('DiagnosisPage', () => {
  const surgical: Diagnosis = {
    id: 3, patient: { id: 7, firstName: 'Lucía', lastName: 'Fernández', dni: '30123456' },
    description: 'Fractura distal de radio izquierdo', createdDate: '2026-10-12',
    treatment: {
      type: 'SURGICAL', surgeryDate: '2026-10-14', stitchesRemovalDate: '2026-10-28',
      surgeryType: { id: 1, name: 'Osteosíntesis con placa', active: true }, hasImmobilization: null, immobilizationDate: null, requiresScarType: true,
    },
    followUpCount: 0, pendingBillingCount: 0,
  };
  const followUp: FollowUp = {
    id: 9, diagnosisId: 3, date: '2026-10-28', scarType: { id: 1, name: 'Normotrófica', active: true },
    billed: false,
    evaluations: [{
      id: 11, segment: { id: 3, name: 'Muñeca', active: true }, edema: 1.5, pain: 3, rom: 55, strength: 14,
      template: { id: 5, name: 'Fractura de muñeca' }, metrics: [],
      answers: [
        { questionId: 1, questionText: '¿Usa férula?', depth: 0, answerType: 'YES_NO', numericValue: null,
          booleanValue: true, optionValue: null, textValue: null },
        { questionId: 2, questionText: 'Horas de uso por día', depth: 1, answerType: 'NUMERIC', numericValue: 8,
          booleanValue: null, optionValue: null, textValue: null },
      ],
    }],
  };
  let get: ReturnType<typeof vi.fn>;
  let listForDiagnosis: ReturnType<typeof vi.fn>;
  let fixture: ComponentFixture<DiagnosisPage>;
  let billing: Record<'summary' | 'register' | 'voidBilling', ReturnType<typeof vi.fn>>;
  const billingSummary: BillingSummary = {
    totalSessions: 2, billedSessions: 1, pendingSessions: 1,
    pending: [{ followUpId: 10, date: '2026-11-04', segments: ['Muñeca'] }],
    billings: [{
      id: 12, billedOn: '2026-11-02', amount: 25000, sessionCount: 1, voided: false, voidedAt: null,
      createdAt: '2026-11-02T13:00:00Z', sessions: [{ followUpId: 9, date: '2026-10-28', segments: ['Muñeca'] }],
    }],
  };

  function render() {
    TestBed.configureTestingModule({
      imports: [DiagnosisPage],
      providers: [
        provideRouter([]),
        { provide: DiagnosesService, useValue: { get } },
        { provide: FollowUpsService, useValue: { listForDiagnosis } },
        { provide: BillingService, useValue: billing },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '7', diagnosisId: '3' }) } } },
      ],
    });
    fixture = TestBed.createComponent(DiagnosisPage);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    get = vi.fn(() => of(surgical));
    listForDiagnosis = vi.fn(() => of([] as FollowUp[]));
    billing = { summary: vi.fn(() => of(billingSummary)), register: vi.fn(), voidBilling: vi.fn() };
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
        hasImmobilization: true, immobilizationDate: '2026-08-02', requiresScarType: false },
    }));
    const el = render();
    expect(el.textContent).toContain('Conservador');
    expect(el.textContent).toContain('Inmovilización');
    expect(el.textContent).not.toContain('Retiro de puntos');
  });

  it('shows "Sin inmovilización" when the conservative diagnosis has none', () => {
    get.mockReturnValue(of({
      ...surgical,
      treatment: { type: 'CONSERVATIVE', surgeryDate: null, stitchesRemovalDate: null, surgeryType: null,
        hasImmobilization: false, immobilizationDate: null, requiresScarType: false },
    }));
    const el = render();
    expect(el.querySelector('.diagnosis-summary__data')?.textContent).toContain('Sin inmovilización');
  });

  it('has the follow-ups section, the new follow-up link and a way back to the patient', () => {
    const el = render();
    expect(listForDiagnosis).toHaveBeenCalledWith(3);
    expect(el.querySelector('#follow-ups-heading')?.textContent).toContain('Seguimientos');
    expect(el.textContent).toContain('Todavía no hay seguimientos.');
    expect(el.querySelector('a.follow-ups__new')?.getAttribute('href')).toBe('/pacientes/7/diagnosticos/3/seguimientos/nuevo');
    expect(el.querySelector('a.page-header__back')?.getAttribute('href')).toBe('/pacientes/7');
  });

  it('lists the follow-ups with their metrics and folded answers', () => {
    listForDiagnosis.mockReturnValue(of([followUp]));
    const el = render();
    const card = el.querySelector('app-follow-up-card') as HTMLElement;
    expect(card.textContent).toContain('28/10/2026');
    expect(card.textContent).toContain('Cicatriz: Normotrófica');
    expect(card.querySelector('.segment__metrics')?.textContent).toContain('1,5');
    const toggle = card.querySelector('.segment__toggle') as HTMLButtonElement;
    expect(toggle.textContent).toContain('Fractura de muñeca · 2 respuestas');
    expect(card.textContent).not.toContain('Horas de uso por día');
  });

  it('renders the billing section before the follow-ups', () => {
    const el = render();
    expect(billing.summary).toHaveBeenCalledWith(3);
    const section = el.querySelector('app-billing-section');
    expect(section?.querySelector('#billing-heading')?.textContent).toContain('Facturación');
    expect(section?.textContent).toContain('$ 25.000,00');
    const followUps = el.querySelector('section.follow-ups')!;
    expect(section!.compareDocumentPosition(followUps) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('reloads the follow-ups when a billing is voided', () => {
    billing.voidBilling.mockReturnValue(of({ ...billingSummary, billedSessions: 0, pendingSessions: 2 }));
    const el = render();
    expect(listForDiagnosis).toHaveBeenCalledTimes(1);

    el.querySelector<HTMLButtonElement>('.billing-row__void')!.click();
    fixture.detectChanges();
    el.querySelector<HTMLButtonElement>('.billing-row__confirm-void')!.click();
    fixture.detectChanges();
    expect(billing.voidBilling).toHaveBeenCalledWith(12);
    expect(listForDiagnosis).toHaveBeenCalledTimes(2);
  });

  it('says so when the diagnosis does not exist', () => {
    get.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    const el = render();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Este diagnóstico no existe.');
  });
});
