import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { BillingSection } from './billing-section';
import { BillingService } from '../billing.service';
import { Billing, BillingSummary } from '../billing.models';

describe('BillingSection', () => {
  const active: Billing = {
    origin: 'APPOINTMENT', id: 12, billedOn: '2026-11-02', amount: 25000, sessionCount: 2, voided: false, voidedAt: null,
    createdAt: '2026-11-02T13:00:00Z',
    sessions: [
      { origin: 'APPOINTMENT', appointmentId: 1, followUpId: null, date: '2026-10-01', startTime: '09:00:00', confirmedAt: '2026-11-09T12:00:00Z', diagnosis: null, segments: [] },
      { origin: 'APPOINTMENT', appointmentId: 2, followUpId: null, date: '2026-10-15', startTime: '09:00:00', confirmedAt: '2026-11-09T12:00:00Z', diagnosis: { id: 3, description: 'Diagnóstico de ejemplo' }, segments: [] },
    ],
  } as Billing;
  const voided: Billing = {
    origin: 'LEGACY_FOLLOW_UP', id: 11, billedOn: '2026-09-28', amount: 15000, sessionCount: 3, voided: true, voidedAt: '2026-09-29T10:00:00Z',
    createdAt: '2026-09-28T10:00:00Z', sessions: [],
  } as Billing;
  const summary: BillingSummary = {
    totalSessions: 4, billedSessions: 2, pendingSessions: 2,
    pending: [
      { origin: 'APPOINTMENT', appointmentId: 3, followUpId: null, date: '2026-11-05', startTime: '09:00:00', confirmedAt: '2026-11-09T12:00:00Z', diagnosis: null, segments: [] },
      { origin: 'APPOINTMENT', appointmentId: 4, followUpId: null, date: '2026-11-06', startTime: '09:00:00', confirmedAt: '2026-11-09T12:00:00Z', diagnosis: { id: 3, description: 'Diagnóstico de ejemplo' }, segments: [] },
    ],
    billings: [active, voided],
  };
  let service: Record<'summary' | 'register' | 'voidBilling', ReturnType<typeof vi.fn>>;

  function setup(initial: BillingSummary = summary, desktop = false) {
    vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: desktop, addEventListener: vi.fn(), removeEventListener: vi.fn() })));
    service = { summary: vi.fn(() => of(initial)), register: vi.fn(), voidBilling: vi.fn() };
    TestBed.configureTestingModule({
      imports: [BillingSection],
      providers: [provideRouter([]), { provide: BillingService, useValue: service }],
    });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(BillingSection);
    fixture.componentRef.setInput('patientId', 7);
    fixture.componentRef.setInput('description', 'Lucía Fernández');
    const changed = vi.fn();
    fixture.componentInstance.changed.subscribe(changed);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const rows = () => Array.from(el.querySelectorAll<HTMLElement>('.billing-row'));
    return { fixture, el, rows, navigate, changed };
  }

  afterEach(() => vi.unstubAllGlobals());

  it('loads the summary and shows the counters', () => {
    const { el } = setup();
    expect(service.summary).toHaveBeenCalledWith(7);
    expect(el.querySelector('#billing-heading')?.textContent).toContain('Facturación');
    const counters = Array.from(el.querySelectorAll('.counter')).map((counter) =>
      `${counter.querySelector('.counter__value')?.textContent?.trim()} ${counter.querySelector('.counter__label')?.textContent?.trim()}`);
    expect(counters).toEqual(['4 turnos confirmados', '2 facturados', '2 pendientes']);
  });

  it('shows the register button only with pending sessions', () => {
    expect(setup().el.querySelector('.billing__register')?.textContent).toContain('Registrar facturación');
    TestBed.resetTestingModule();
    const { el } = setup({ ...summary, billedSessions: 4, pendingSessions: 0, pending: [] });
    expect(el.querySelector('.billing__register')).toBeNull();
  });

  it('says so when there are no sessions', () => {
    const { el } = setup({ totalSessions: 0, billedSessions: 0, pendingSessions: 0, pending: [], billings: [] });
    expect(el.textContent).toContain('Todavía no hay turnos confirmados para facturar.');
    expect(el.textContent).not.toContain('Facturaciones anteriores');
  });

  it('lists each billing with its count, formatted amount and long date', () => {
    const { el, rows } = setup();
    expect(el.textContent).toContain('Facturaciones anteriores');
    const first = rows()[0];
    expect(first.querySelector('.billing-row__count')?.textContent).toContain('2 sesiones');
    expect(first.querySelector('.billing-row__amount')?.textContent).toContain('$ 25.000,00');
    expect(first.querySelector('.billing-row__date')?.textContent).toContain('Facturado el 2 de noviembre de 2026');
  });

  it('toggles the sessions of a billing', () => {
    const { fixture, rows } = setup();
    const toggle = rows()[0].querySelector<HTMLButtonElement>('.billing-row__toggle')!;
    expect(toggle.textContent).toContain('Ver sesiones');
    expect(rows()[0].querySelector('.billing-row__sessions')).toBeNull();

    toggle.click();
    fixture.detectChanges();
    expect(toggle.textContent).toContain('Ocultar sesiones');
    const sessions = rows()[0].querySelector('.billing-row__sessions')!.textContent ?? '';
    expect(sessions).toContain('01/10/2026');
    expect(sessions).toContain('09:00');
    expect(sessions).toContain('Confirmado');
    expect(sessions).toContain('Diagnóstico de ejemplo');

    toggle.click();
    fixture.detectChanges();
    expect(toggle.textContent).toContain('Ver sesiones');
    expect(rows()[0].querySelector('.billing-row__sessions')).toBeNull();
  });

  it('preserves previous records and their segments without adding them to appointment counters', () => {
    const legacy: Billing = {
      ...active, id: 19, origin: 'LEGACY_FOLLOW_UP', sessionCount: 1,
      sessions: [{ origin: 'LEGACY_FOLLOW_UP', appointmentId: null, followUpId: 3,
        date: '2026-10-01', startTime: null, confirmedAt: null, diagnosis: null, segments: ['Muñeca', 'Mano'] }],
    };
    const { fixture, el, rows } = setup({ ...summary, billings: [legacy, active] });
    expect(rows()[0].textContent).toContain('Registro anterior');
    expect(rows()[1].textContent).not.toContain('Registro anterior');
    expect(el.querySelector('.counter--pending .counter__value')?.textContent).toBe('2');
    rows()[0].querySelector<HTMLButtonElement>('.billing-row__toggle')!.click();
    fixture.detectChanges();
    expect(rows()[0].querySelector('.billing-row__sessions')?.textContent).toContain('01/10/2026');
    expect(rows()[0].querySelector('.billing-row__sessions')?.textContent).toContain('Muñeca, Mano');
    expect(rows()[0].querySelector('.billing-row__sessions')?.textContent).not.toContain('Confirmado');
    rows()[0].querySelector<HTMLButtonElement>('.billing-row__void')!.click();
    fixture.detectChanges();
    expect(rows()[0].querySelector('.billing-row__confirm')?.textContent).not.toContain('vuelven a quedar pendientes');
    service.voidBilling.mockReturnValue(of({ ...summary, billings: [{ ...legacy, voided: true }, active] }));
    rows()[0].querySelector<HTMLButtonElement>('.billing-row__confirm-void')!.click();
    fixture.detectChanges();
    expect(service.voidBilling).toHaveBeenCalledWith(19);
    expect(rows()[0].textContent).toContain('Anulada');
    expect(rows()[0].querySelector('.billing-row__sessions')?.textContent).toContain('Muñeca, Mano');
    expect(el.querySelector('.counter--pending .counter__value')?.textContent).toBe('2');
  });

  it('voids a billing after the inline confirmation', () => {
    const afterVoid: BillingSummary = { ...summary, billedSessions: 0, pendingSessions: 4, billings: [{ ...active, voided: true }, voided] };
    const { fixture, rows, changed } = setup();
    service.voidBilling.mockReturnValue(of(afterVoid));

    rows()[0].querySelector<HTMLButtonElement>('.billing-row__void')!.click();
    fixture.detectChanges();
    const confirm = rows()[0].querySelector('.billing-row__confirm')!;
    expect(confirm.textContent).toContain('¿Anular esta facturación? Sus sesiones vuelven a quedar pendientes.');
    expect(service.voidBilling).not.toHaveBeenCalled();

    confirm.querySelector<HTMLButtonElement>('.billing-row__confirm-void')!.click();
    fixture.detectChanges();
    expect(service.voidBilling).toHaveBeenCalledWith(12);
    expect(changed).toHaveBeenCalled();
    expect(rows()[0].querySelector('.billing-row__confirm')).toBeNull();
    expect(rows()[0].textContent).toContain('Anulada');
  });

  it('keeps the record and allows retry after a failed void', () => {
    const { fixture, el, rows, changed } = setup();
    service.voidBilling.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    rows()[0].querySelector<HTMLButtonElement>('.billing-row__void')!.click();
    fixture.detectChanges();
    rows()[0].querySelector<HTMLButtonElement>('.billing-row__confirm-void')!.click();
    fixture.detectChanges();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('No pudimos anular');
    expect(rows()[0].querySelector('.billing-row__voided')).toBeNull();
    expect(rows()[0].querySelector<HTMLButtonElement>('.billing-row__confirm-void')?.disabled).toBe(false);
    expect(changed).not.toHaveBeenCalled();
    expect(el.querySelector('.counter--pending .counter__value')?.textContent).toBe('2');
  });

  it('cancels the void confirmation', () => {
    const { fixture, rows } = setup();
    rows()[0].querySelector<HTMLButtonElement>('.billing-row__void')!.click();
    fixture.detectChanges();
    rows()[0].querySelector<HTMLButtonElement>('.billing-row__cancel')!.click();
    fixture.detectChanges();
    expect(rows()[0].querySelector('.billing-row__confirm')).toBeNull();
    expect(service.voidBilling).not.toHaveBeenCalled();
  });

  it('shows a voided billing as "Anulada" without "Anular"', () => {
    const { rows } = setup();
    const row = rows()[1];
    expect(row.classList).toContain('billing-row--voided');
    expect(row.querySelector('.billing-row__voided')?.textContent).toContain('Anulada');
    expect(row.querySelector('.billing-row__void')).toBeNull();
    expect(row.querySelector('.billing-row__amount')).toBeNull();
    expect(row.querySelector('.billing-row__toggle')?.textContent).toContain('Ver sesiones');
  });

  it('navigates to the form page on mobile', () => {
    const { el, navigate } = setup();
    el.querySelector<HTMLButtonElement>('.billing__register')!.click();
    expect(navigate).toHaveBeenCalledWith(['/pacientes', 7, 'facturacion', 'nueva']);
    expect(el.querySelector('[role="dialog"]')).toBeNull();
  });

  it('opens the side panel on desktop with every pending session checked', () => {
    const { fixture, el, navigate } = setup(summary, true);
    el.querySelector<HTMLButtonElement>('.billing__register')!.click();
    fixture.detectChanges();
    const panel = el.querySelector('[role="dialog"]')!;
    expect(navigate).not.toHaveBeenCalled();
    expect(panel.getAttribute('aria-modal')).toBe('true');
    expect(panel.textContent).toContain('Registrar facturación');
    expect(panel.textContent).toContain('Lucía Fernández');
    const boxes = Array.from(panel.querySelectorAll<HTMLInputElement>('input[type="checkbox"]'));
    expect(boxes.map((box) => box.checked)).toEqual([true, true]);
  });

  it('re-creates the form on each opening and refreshes after saving', () => {
    const saved: BillingSummary = { ...summary, billedSessions: 4, pendingSessions: 0, pending: [] };
    const { fixture, el, changed } = setup(summary, true);
    el.querySelector<HTMLButtonElement>('.billing__register')!.click();
    fixture.detectChanges();
    el.querySelector<HTMLInputElement>('[role="dialog"] input[type="checkbox"]')!.click();
    fixture.detectChanges();
    el.querySelector<HTMLButtonElement>('.billing-form__cancel')!.click();
    fixture.detectChanges();
    expect(el.querySelector('[role="dialog"]')).toBeNull();

    el.querySelector<HTMLButtonElement>('.billing__register')!.click();
    fixture.detectChanges();
    const boxes = Array.from(el.querySelectorAll<HTMLInputElement>('[role="dialog"] input[type="checkbox"]'));
    expect(boxes.map((box) => box.checked)).toEqual([true, true]);

    service.register.mockReturnValue(of(saved));
    const amount = el.querySelector<HTMLInputElement>('#billing-amount')!;
    amount.value = '20000';
    amount.dispatchEvent(new Event('input'));
    el.querySelector<HTMLFormElement>('.billing-form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    expect(service.register).toHaveBeenCalledWith(7, expect.objectContaining({ amount: 20000, appointmentIds: [3, 4] }));
    expect(el.querySelector('[role="dialog"]')).toBeNull();
    expect(changed).toHaveBeenCalled();
    expect(el.querySelector('.billing__register')).toBeNull();
  });

  it('re-fetches the summary on a stale 422 and re-creates the form with the fresh pending list and the message', () => {
    const fresh: BillingSummary = {
      ...summary, billedSessions: 3, pendingSessions: 1,
      pending: [{ origin: 'APPOINTMENT', appointmentId: 4, followUpId: null, date: '2026-11-06', startTime: '09:00:00', confirmedAt: '2026-11-09T12:00:00Z', diagnosis: { id: 3, description: 'Diagnóstico de ejemplo' }, segments: [] }],
    };
    const { fixture, el, changed } = setup(summary, true);
    el.querySelector<HTMLButtonElement>('.billing__register')!.click();
    fixture.detectChanges();
    expect(el.querySelectorAll('[role="dialog"] input[type="checkbox"]').length).toBe(2);

    service.summary.mockReturnValue(of(fresh));
    service.register.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 422, error: { code: 'BILLING_SESSION_INVALID', message: 'Hay sesiones ya facturadas.' },
    })));
    const amount = el.querySelector<HTMLInputElement>('#billing-amount')!;
    amount.value = '20000';
    amount.dispatchEvent(new Event('input'));
    el.querySelector<HTMLFormElement>('.billing-form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(service.summary).toHaveBeenCalledTimes(2);
    const boxes = Array.from(el.querySelectorAll<HTMLInputElement>('[role="dialog"] input[type="checkbox"]'));
    expect(boxes.map((box) => box.checked)).toEqual([true]);
    expect(el.querySelector('[role="dialog"] .billing-form__body > [role="alert"]')?.textContent).toContain('ya estaban facturadas');
    expect(el.querySelector('.counter--pending')?.textContent).toContain('1');
    expect(changed).toHaveBeenCalled();
  });
});
