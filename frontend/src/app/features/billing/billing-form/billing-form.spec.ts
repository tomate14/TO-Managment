import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { BillingForm } from './billing-form';
import { BillingService } from '../billing.service';
import { BilledSessionRef, BillingSummary } from '../billing.models';
import { todayIso } from '../../patients/patient-format';

describe('BillingForm', () => {
  const pending: BilledSessionRef[] = [
    { origin: 'APPOINTMENT', appointmentId: 21, followUpId: null, date: '2026-11-05', startTime: '09:00:00', confirmedAt: '2026-11-09T12:00:00Z', diagnosis: null, segments: [] },
    { origin: 'APPOINTMENT', appointmentId: 22, followUpId: null, date: '2026-11-06', startTime: '09:00:00', confirmedAt: '2026-11-09T12:00:00Z', diagnosis: { id: 3, description: 'Diagnóstico de ejemplo' }, segments: [] },
    { origin: 'APPOINTMENT', appointmentId: 23, followUpId: null, date: '2026-11-07', startTime: '09:00:00', confirmedAt: '2026-11-09T12:00:00Z', diagnosis: { id: 4, description: 'Otro diagnóstico' }, segments: [] },
  ];
  const summary: BillingSummary = { totalSessions: 3, billedSessions: 3, pendingSessions: 0, pending: [], billings: [] };
  let register: ReturnType<typeof vi.fn>;

  function render(sessions: BilledSessionRef[] = pending) {
    register = vi.fn(() => of(summary));
    TestBed.configureTestingModule({
      imports: [BillingForm],
      providers: [{ provide: BillingService, useValue: { register } }],
    });
    const fixture = TestBed.createComponent(BillingForm);
    fixture.componentRef.setInput('patientId', 7);
    fixture.componentRef.setInput('pending', sessions);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const checkboxes = () => Array.from(el.querySelectorAll<HTMLInputElement>('input[type="checkbox"]'));
    const typeAmount = (value: string) => {
      const input = el.querySelector<HTMLInputElement>('#billing-amount')!;
      input.value = value;
      input.dispatchEvent(new Event('input'));
      fixture.detectChanges();
    };
    const submit = () => {
      el.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
      fixture.detectChanges();
    };
    return { fixture, el, checkboxes, typeAmount, submit };
  }

  it('starts with every pending session checked, today as the date and an empty amount', () => {
    const { el, checkboxes } = render();
    expect(checkboxes().map((box) => box.checked)).toEqual([true, true, true]);
    expect(el.textContent).toContain('05/11/2026');
    expect(el.textContent).toContain('09:00');
    expect(el.querySelectorAll('.session-option__diagnosis')).toHaveLength(2);
    expect(el.querySelectorAll('.session-option__confirmed')).toHaveLength(3);
    expect(el.textContent).not.toContain('Sin diagnóstico');
    expect(el.textContent).not.toContain('Muñeca');
    const date = el.querySelector<HTMLInputElement>('#billing-date')!;
    expect(date.value).toBe(todayIso());
    expect(date.getAttribute('max')).toBe(todayIso());
    expect(el.querySelector<HTMLInputElement>('#billing-amount')!.value).toBe('');
    expect(el.querySelector('#billing-amount')!.getAttribute('inputmode')).toBe('decimal');
  });

  it('only allows appointment references with an appointment id, never historical follow-up ids', () => {
    const legacy: BilledSessionRef = { origin: 'LEGACY_FOLLOW_UP', appointmentId: null, followUpId: 21, date: '2026-11-01', startTime: null, confirmedAt: null, diagnosis: null, segments: ['Mano'] };
    const invalid = { ...pending[0], appointmentId: null } as unknown as BilledSessionRef;
    const { checkboxes, typeAmount, submit } = render([pending[0], legacy, invalid]);
    expect(checkboxes()).toHaveLength(1);
    typeAmount('1000');
    submit();
    expect(register).toHaveBeenCalledWith(7, { billedOn: todayIso(), amount: 1000, appointmentIds: [21] });
  });

  it('counts the chosen sessions', () => {
    const { fixture, el, checkboxes } = render();
    expect(el.querySelector('.billing-form__count')?.textContent).toContain('3 de 3 elegidas');
    checkboxes()[1].click();
    fixture.detectChanges();
    expect(el.querySelector('.billing-form__count')?.textContent).toContain('2 de 3 elegidas');
  });

  it('asks for at least one session', () => {
    const { fixture, el, checkboxes, typeAmount, submit } = render();
    checkboxes().forEach((box) => box.click());
    fixture.detectChanges();
    typeAmount('1000');
    submit();
    expect(el.textContent).toContain('Elegí al menos una sesión.');
    expect(register).not.toHaveBeenCalled();
  });

  it('asks for the amount', () => {
    const { el, submit } = render();
    submit();
    expect(el.querySelector('#billing-amount-error')?.textContent).toContain('Ingresá el monto.');
    expect(register).not.toHaveBeenCalled();
  });

  it('rejects an amount that is not a number or is zero', () => {
    const { el, typeAmount, submit } = render();
    typeAmount('abc');
    submit();
    expect(el.querySelector('#billing-amount-error')?.textContent).toContain('El monto tiene que ser mayor a cero.');
    typeAmount('0');
    expect(el.querySelector('#billing-amount-error')?.textContent).toContain('El monto tiene que ser mayor a cero.');
    expect(register).not.toHaveBeenCalled();
  });

  it('mirrors the backend digit limit', () => {
    const { el, typeAmount, submit } = render();
    typeAmount('25000,505');
    submit();
    expect(el.querySelector('#billing-amount-error')?.textContent).toContain('El monto no es válido.');
    expect(register).not.toHaveBeenCalled();
  });

  it('sends the chosen sessions, the date and the parsed amount, and emits the summary', () => {
    const { fixture, checkboxes, typeAmount, submit } = render();
    const saved = vi.fn();
    fixture.componentInstance.saved.subscribe(saved);
    checkboxes()[2].click();
    fixture.detectChanges();
    typeAmount('25.000,50');
    submit();
    expect(register).toHaveBeenCalledWith(7, { billedOn: todayIso(), amount: 25000.5, appointmentIds: [21, 22] });
    expect(saved).toHaveBeenCalledWith(summary);
  });

  it('shows the BILLING_SESSION_INVALID message on top', () => {
    const { el, typeAmount, submit } = render();
    register.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 422,
      error: { code: 'BILLING_SESSION_INVALID', message: 'Hay sesiones que no son de este paciente, no están confirmadas o ya están facturadas.' },
    })));
    typeAmount('1000');
    submit();
    expect(el.querySelector('.billing-form__body > [role="alert"]')?.textContent)
      .toContain('Hay sesiones que no son de este paciente, no están confirmadas o ya están facturadas.');
  });

  it('emits stale on BILLING_SESSION_INVALID and keeps the message', () => {
    const { fixture, el, typeAmount, submit } = render();
    const stale = vi.fn();
    fixture.componentInstance.stale.subscribe(stale);
    register.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 422, error: { code: 'BILLING_SESSION_INVALID', message: 'Ya facturadas.' },
    })));
    typeAmount('1000');
    submit();
    expect(stale).toHaveBeenCalledTimes(1);
    expect(el.querySelector('.billing-form__body > [role="alert"]')?.textContent).toContain('Ya facturadas.');
  });

  it('shows the notice it was created with', () => {
    TestBed.configureTestingModule({
      imports: [BillingForm],
      providers: [{ provide: BillingService, useValue: { register: vi.fn() } }],
    });
    const fixture = TestBed.createComponent(BillingForm);
    fixture.componentRef.setInput('patientId', 7);
    fixture.componentRef.setInput('pending', pending);
    fixture.componentRef.setInput('notice', 'Actualizamos la lista.');
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('.billing-form__body > [role="alert"]')?.textContent)
      .toContain('Actualizamos la lista.');
  });

  it('shows the backend field errors under their fields', () => {
    const { el, typeAmount, submit } = render();
    register.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 400,
      error: { code: 'VALIDATION_ERROR', message: 'Revisá los datos.', fieldErrors: [{ field: 'amount', message: 'El monto no es válido.' }] },
    })));
    typeAmount('1000');
    submit();
    expect(el.querySelector('#billing-amount-error')?.textContent).toContain('El monto no es válido.');
  });

  it('emits cancelled', () => {
    const { fixture, el } = render();
    const cancelled = vi.fn();
    fixture.componentInstance.cancelled.subscribe(cancelled);
    el.querySelector<HTMLButtonElement>('.billing-form__cancel')!.click();
    expect(cancelled).toHaveBeenCalled();
  });
});
