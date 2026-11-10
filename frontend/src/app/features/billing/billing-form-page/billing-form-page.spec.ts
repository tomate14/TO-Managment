import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { BillingFormPage } from './billing-form-page';
import { BillingService } from '../billing.service';
import { BillingSummary } from '../billing.models';
import { PatientsService } from '../../patients/patients.service';
import { Patient } from '../../patients/patients.models';

describe('BillingFormPage', () => {
  const patient: Patient = { id: 7, firstName: 'Lucía', lastName: 'Fernández', dni: '30123456', birthDate: '1990-01-01', phone: '1234', occupation: 'Docente', healthInsurance: '', healthInsuranceNumber: '', dominance: 'RIGHT' };
  const summary: BillingSummary = {
    totalSessions: 2, billedSessions: 0, pendingSessions: 2,
    pending: [
      { origin: 'APPOINTMENT', appointmentId: 21, followUpId: null, date: '2026-11-05', startTime: '09:00:00', confirmedAt: '2026-11-09T12:00:00Z', diagnosis: null, segments: [] },
      { origin: 'APPOINTMENT', appointmentId: 22, followUpId: null, date: '2026-11-06', startTime: '09:00:00', confirmedAt: '2026-11-09T12:00:00Z', diagnosis: { id: 3, description: 'Diagnóstico de ejemplo' }, segments: [] },
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
        { provide: PatientsService, useValue: { get: vi.fn(() => of(patient)) } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '7' }) } } },
      ],
    });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(BillingFormPage);
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement, navigate };
  }

  it('shows the title, the patient, and the pending sessions checked', () => {
    const { el } = setup();
    expect(summaryCall).toHaveBeenCalledWith(7);
    expect(el.querySelector('h1')?.textContent).toContain('Registrar facturación');
    expect(el.textContent).toContain('Lucía Fernández');
    const boxes = Array.from(el.querySelectorAll<HTMLInputElement>('input[type="checkbox"]'));
    expect(boxes.map((box) => box.checked)).toEqual([true, true]);
    expect(el.querySelector('a.page-header__back')?.getAttribute('href')).toBe('/pacientes/7');
  });

  it('returns to the patient when cancelled', () => {
    const { el, navigate } = setup();
    el.querySelector<HTMLButtonElement>('.billing-form__cancel')!.click();
    expect(navigate).toHaveBeenCalledWith(['/pacientes', 7]);
  });

  it('returns to the patient after registering its selected appointments', () => {
    const { fixture, el, navigate } = setup();
    const amount = el.querySelector<HTMLInputElement>('#billing-amount')!;
    amount.value = '1000';
    amount.dispatchEvent(new Event('input'));
    el.querySelector<HTMLFormElement>('.billing-form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    expect(register).toHaveBeenCalledWith(7, expect.objectContaining({ appointmentIds: [21, 22], amount: 1000 }));
    expect(navigate).toHaveBeenCalledWith(['/pacientes', 7]);
  });

  it('says so when there is nothing to bill', () => {
    const { el } = setup({ ...summary, pendingSessions: 0, pending: [] });
    expect(el.textContent).toContain('Todavía no hay turnos confirmados para facturar.');
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
