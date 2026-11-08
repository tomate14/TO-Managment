import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { BillingFormPage } from './billing-form-page';
import { BillingService } from '../billing.service';
import { BillingSummary } from '../billing.models';
import { DiagnosesService } from '../../diagnoses/diagnoses.service';
import { Diagnosis } from '../../diagnoses/diagnoses.models';

describe('BillingFormPage', () => {
  const diagnosis: Diagnosis = {
    id: 3, patient: { id: 7, firstName: 'Lucía', lastName: 'Fernández', dni: '30123456' },
    description: 'Tendinitis de muñeca', createdDate: '2026-09-02',
    treatment: { type: 'CONSERVATIVE', surgeryDate: null, stitchesRemovalDate: null, surgeryType: null,
      hasImmobilization: false, immobilizationDate: null, requiresScarType: false },
    followUpCount: 2, pendingBillingCount: 2,
  };
  const summary: BillingSummary = {
    totalSessions: 2, billedSessions: 0, pendingSessions: 2,
    pending: [
      { followUpId: 21, date: '2026-11-05', segments: ['Muñeca'] },
      { followUpId: 22, date: '2026-11-06', segments: ['Muñeca'] },
    ],
    billings: [],
  };

  let register: ReturnType<typeof vi.fn>;
  let summaryCall: ReturnType<typeof vi.fn>;

  function setup(initial: BillingSummary = summary) {
    register = vi.fn(() => of(initial));
    summaryCall = vi.fn(() => of(initial));
    TestBed.configureTestingModule({
      imports: [BillingFormPage],
      providers: [
        provideRouter([]),
        { provide: BillingService, useValue: { summary: summaryCall, register } },
        { provide: DiagnosesService, useValue: { get: vi.fn(() => of(diagnosis)) } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '7', diagnosisId: '3' }) } } },
      ],
    });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(BillingFormPage);
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement, navigate };
  }

  it('shows the title, the diagnosis and patient, and the pending sessions checked', () => {
    const { el } = setup();
    expect(el.querySelector('h1')?.textContent).toContain('Registrar facturación');
    expect(el.textContent).toContain('Tendinitis de muñeca · Lucía Fernández');
    const boxes = Array.from(el.querySelectorAll<HTMLInputElement>('input[type="checkbox"]'));
    expect(boxes.map((box) => box.checked)).toEqual([true, true]);
    expect(el.querySelector('a.page-header__back')?.getAttribute('href')).toBe('/pacientes/7/diagnosticos/3');
  });

  it('returns to the diagnosis when cancelled', () => {
    const { el, navigate } = setup();
    el.querySelector<HTMLButtonElement>('.billing-form__cancel')!.click();
    expect(navigate).toHaveBeenCalledWith(['/pacientes', 7, 'diagnosticos', 3]);
  });

  it('says so when there is nothing to bill', () => {
    const { el } = setup({ ...summary, pendingSessions: 0, pending: [] });
    expect(el.textContent).toContain('Todavía no hay sesiones para facturar.');
    expect(el.querySelector('app-billing-form')).toBeNull();
  });

  it('re-fetches the summary on a stale 422 and re-renders the form with the fresh list and the message', () => {
    const { fixture, el } = setup();
    summaryCall.mockReturnValue(of({ ...summary, pendingSessions: 1, pending: [summary.pending[1]] }));
    register.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 422, error: { code: 'BILLING_SESSION_INVALID', message: 'Hay sesiones ya facturadas.' },
    })));
    const amount = el.querySelector<HTMLInputElement>('#billing-amount')!;
    amount.value = '20000';
    amount.dispatchEvent(new Event('input'));
    el.querySelector<HTMLFormElement>('.billing-form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(summaryCall).toHaveBeenCalledTimes(2);
    expect(el.querySelectorAll('input[type="checkbox"]').length).toBe(1);
    expect(el.querySelector('.billing-form__body > [role="alert"]')?.textContent).toContain('ya estaban facturadas');
  });
});
