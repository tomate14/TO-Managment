import { vi } from 'vitest';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Subject, of } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { DiagnosesService } from '../../diagnoses/diagnoses.service';
import { PatientsService } from '../../patients/patients.service';
import { AGENDA_TODAY } from '../agenda-dates';
import { appointmentOf } from '../agenda-test-data';
import { Appointment } from '../agenda.models';
import { AgendaService } from '../agenda.service';
import { AgendaPage } from './agenda-page';

describe('Agenda appointment confirmation', () => {
  afterEach(() => vi.unstubAllGlobals());

  async function setup(reads?: Subject<Appointment[]>[], options: { desktop?: boolean; appointments?: Appointment[] } = {}) {
    if (options.desktop) {
      vi.stubGlobal('matchMedia', () => ({ matches: true, addEventListener() {}, removeEventListener() {} }));
    }
    const appointments = options.appointments ?? [appointmentOf({ id: 1 }), appointmentOf({ id: 2 })];
    const response = new Subject<Appointment>();
    const confirm = vi.fn().mockReturnValue(response);
    const cancel = vi.fn().mockReturnValue(of(undefined));
    let readIndex = 0;
    const list = vi.fn().mockImplementation(() => reads ? reads[readIndex++] : of(appointments));
    TestBed.configureTestingModule({
      imports: [AgendaPage],
      providers: [
        provideRouter([]),
        { provide: AGENDA_TODAY, useValue: () => '2026-11-05' },
        { provide: AgendaService, useValue: { list, confirm, cancel, therapists: () => of([]), availability: () => of([]) } },
        { provide: DiagnosesService, useValue: { listForPatient: () => of([]) } },
        { provide: PatientsService, useValue: {} },
        { provide: AuthService, useValue: { currentUser: signal({ id: 1, email: 'example@test.com', fullName: 'Ana', role: 'TERAPEUTA' }) } },
      ],
    });
    const router = TestBed.inject(Router);
    await router.navigate([], { queryParams: { vista: 'turnos' } });
    const fixture = TestBed.createComponent(AgendaPage);
    fixture.detectChanges();
    TestBed.tick();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const cards = () => el.querySelectorAll<HTMLElement>('app-appointment-card');
    const button = (index = 0) => cards()[index].querySelector<HTMLButtonElement>('[aria-label="Confirmar sesión"]')!;
    return { fixture, el, cards, button, response, confirm, appointments, router, list };
  }

  it('sends one request on double click and isolates busy state without optimistic success', async () => {
    const { fixture, cards, button, confirm } = await setup();
    expect(button()).toBeTruthy();
    button().click();
    button().click();
    fixture.detectChanges();
    expect(confirm).toHaveBeenCalledExactlyOnceWith(1);
    expect(button().disabled).toBe(true);
    expect(button().getAttribute('aria-busy')).toBe('true');
    expect(button(1).disabled).toBe(false);
    expect(cards()[0].textContent).not.toContain('Confirmado');
    expect(cards()[0].textContent).toContain('Reprogramar');
  });

  it('keeps actions and an error on failure, then allows a successful retry', async () => {
    const { fixture, cards, button, response, confirm, appointments } = await setup();
    expect(button()).toBeTruthy();
    button().click();
    response.error(new Error('offline'));
    fixture.detectChanges();
    expect(cards()[0].querySelector('[role="alert"]')?.textContent).toContain('No pudimos confirmar la sesión');
    expect(cards()[1].querySelector('[role="alert"]')).toBeNull();
    expect(button().disabled).toBe(false);
    expect(cards()[0].textContent).toContain('Reprogramar');
    expect(cards()[0].textContent).not.toContain('Confirmado');
    confirm.mockReturnValue(of({ ...appointments[0], confirmedAt: '2026-11-05T12:00:00Z', billed: false }));
    button().click();
    fixture.detectChanges();
    expect(confirm).toHaveBeenCalledTimes(2);
    expect(cards()[0].textContent).toContain('Confirmado · Pendiente de facturar');
    expect(cards()[0].querySelector('[role="alert"]')).toBeNull();
    expect(cards()[0].querySelector('button')).toBeNull();
  });

  it('updates list and calendar source from the response and opens the confirmed patient record', async () => {
    const { fixture, cards, button, response, appointments, router } = await setup();
    expect(button()).toBeTruthy();
    button().click();
    response.next({ ...appointments[0], ...{ confirmedAt: '2026-11-05T12:00:00Z', billed: true } });
    response.complete();
    fixture.detectChanges();
    expect(cards()[0].textContent).toContain('Confirmado · Facturado');
    await router.navigate([], { queryParams: { vista: 'calendario' } });
    fixture.detectChanges();
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const calendar = fixture.nativeElement.querySelector('.agenda-block') as HTMLButtonElement;
    calendar.click();
    expect(navigate).toHaveBeenCalledWith(['/pacientes', 10]);
    expect(fixture.nativeElement.querySelector('app-appointment-form')).toBeNull();
  });

  it('preserves an accepted retry when older calendar and current-week responses arrive, then accepts fresh billing state', async () => {
    const initialCalendar = new Subject<Appointment[]>();
    const initialCurrentWeek = new Subject<Appointment[]>();
    const oldCalendar = new Subject<Appointment[]>();
    const oldCurrentWeek = new Subject<Appointment[]>();
    const freshCalendar = new Subject<Appointment[]>();
    const freshCurrentWeek = new Subject<Appointment[]>();
    const { fixture, cards, button, response, confirm, appointments, router, list } = await setup([
      initialCalendar, initialCurrentWeek, oldCalendar, oldCurrentWeek, freshCalendar, freshCurrentWeek,
    ]);
    initialCalendar.next(appointments);
    initialCalendar.complete();
    initialCurrentWeek.next(appointments);
    initialCurrentWeek.complete();
    fixture.detectChanges();

    fixture.componentInstance.reload();
    TestBed.tick();
    expect(list).toHaveBeenCalledTimes(4);
    button().click();
    response.error(new Error('offline'));
    fixture.detectChanges();
    expect(button().disabled).toBe(false);
    expect(cards()[0].querySelector('[role="alert"]')).toBeTruthy();

    const retry = new Subject<Appointment>();
    confirm.mockReturnValue(retry);
    button().click();
    button().click();
    const confirmed = { ...appointments[0], confirmedAt: '2026-11-05T12:00:00Z', billed: false };
    retry.next(confirmed);
    retry.complete();
    fixture.detectChanges();
    expect(confirm).toHaveBeenCalledTimes(2);
    expect(cards()[0].textContent).toContain('Confirmado · Pendiente de facturar');

    oldCurrentWeek.next(appointments);
    oldCurrentWeek.complete();
    fixture.detectChanges();
    expect(cards()[0].textContent).toContain('Confirmado · Pendiente de facturar');
    expect(cards()[0].querySelector('button')).toBeNull();

    oldCalendar.next(appointments);
    oldCalendar.complete();
    await router.navigate([], { queryParams: { vista: 'calendario' } });
    fixture.detectChanges();
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    (fixture.nativeElement.querySelector('.agenda-block') as HTMLButtonElement).click();
    expect(navigate).toHaveBeenCalledWith(['/pacientes', 10]);
    navigate.mockRestore();

    // A late event can still carry the card's original object; it must not send a third POST.
    fixture.componentInstance['confirmAppointment'](appointments[0]);
    expect(confirm).toHaveBeenCalledTimes(2);

    fixture.componentInstance.reload();
    TestBed.tick();
    expect(list).toHaveBeenCalledTimes(6);
    const billed = { ...confirmed, billed: true };
    freshCalendar.next([billed, appointments[1]]);
    freshCalendar.complete();
    freshCurrentWeek.next([billed, appointments[1]]);
    freshCurrentWeek.complete();
    expect(fixture.componentInstance['appointments']()[0].billed).toBe(true);
    await router.navigate([], { queryParams: { vista: 'turnos' } });
    fixture.detectChanges();
    expect(cards()[0].textContent).toContain('Confirmado · Facturado');
    expect(cards()[0].querySelector('button')).toBeNull();
  });

  it('confirms a previous-week calendar selection in the desktop panel with unchanged date and retry', async () => {
    const past = appointmentOf({ id: 9, date: '2026-10-29' });
    const { fixture, cards, response, confirm, router } = await setup(undefined, { desktop: true, appointments: [past] });
    expect(cards()).toHaveLength(0); // Assigned-list filters still exclude this appointment.
    await router.navigate([], { queryParams: { vista: 'calendario' } });
    fixture.componentInstance['shiftWeek'](-1);
    TestBed.tick();
    (fixture.nativeElement.querySelector('.calendar-desktop .agenda-block') as HTMLButtonElement).click();
    fixture.detectChanges();
    const panel = () => fixture.nativeElement.querySelector('.booking-panel') as HTMLElement;
    const button = () => panel().querySelector<HTMLButtonElement>('[aria-label="Confirmar sesión"]')!;
    expect(button()).toBeTruthy();
    expect(panel().querySelector('time')?.getAttribute('datetime')).toBe('2026-10-29');
    expect(panel().textContent).toContain('09:00');
    expect(panel().querySelector('app-appointment-form')).toBeNull();
    button().click();
    button().click();
    fixture.detectChanges();
    expect(confirm).toHaveBeenCalledExactlyOnceWith(9);
    expect(panel().querySelector<HTMLButtonElement>('.appointment-card__btn')?.disabled).toBe(true);
    response.error(new Error('offline'));
    fixture.detectChanges();
    expect(panel().querySelector('[role="alert"]')).toBeTruthy();
    expect(button().disabled).toBe(false);
    confirm.mockReturnValue(of({ ...past, confirmedAt: '2026-11-05T12:00:00Z' }));
    button().click();
    fixture.detectChanges();
    expect(panel().textContent).toContain('Confirmado · Pendiente de facturar');
    expect(panel().querySelector('.appointment-card__actions')?.textContent?.trim()).toBe('Ver ficha');
    expect(fixture.componentInstance['appointments']()[0].date).toBe('2026-10-29');
    expect(fixture.componentInstance['appointments']()[0].confirmedAt).toBeTruthy();
  });

  it('opens yesterday from the mobile calendar in the existing page with confirmation details', async () => {
    const { fixture, router } = await setup(undefined, { appointments: [appointmentOf({ id: 9, date: '2026-11-04' })] });
    await router.navigate([], { queryParams: { vista: 'calendario' } });
    fixture.componentInstance['anchorDate'].set('2026-11-04');
    TestBed.tick();
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    (fixture.nativeElement.querySelector('.calendar-mobile .agenda-block') as HTMLButtonElement).click();
    expect(navigate).toHaveBeenCalledWith(['/agenda', 9, 'reprogramar'], { queryParams: { vista: 'calendario', detalle: '1' } });
  });

  it('closes a cancelled detail while preserving an unrelated open booking form', async () => {
    const { fixture, appointments } = await setup(undefined, { desktop: true });
    const page = fixture.componentInstance;
    page['newAppointment']();
    page['cancelAppointment'](appointments[0]);
    expect(page['formOpen']()?.mode).toBe('new');
    page['selectAppointment'](appointments[1]);
    page['cancelAppointment'](appointments[1]);
    expect(page['formOpen']()).toBeNull();
  });
});
