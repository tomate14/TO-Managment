import { vi } from 'vitest';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Subject, of } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { AGENDA_TODAY } from '../agenda-dates';
import { appointmentOf } from '../agenda-test-data';
import { Appointment } from '../agenda.models';
import { AgendaService } from '../agenda.service';
import { AgendaPage } from './agenda-page';

describe('Agenda appointment confirmation', () => {
  async function setup() {
    const appointments = [appointmentOf({ id: 1 }), appointmentOf({ id: 2 })];
    const response = new Subject<Appointment>();
    const confirm = vi.fn().mockReturnValue(response);
    TestBed.configureTestingModule({
      imports: [AgendaPage],
      providers: [
        provideRouter([]),
        { provide: AGENDA_TODAY, useValue: () => '2026-11-05' },
        { provide: AgendaService, useValue: { list: () => of(appointments), confirm, therapists: () => of([]) } },
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
    return { fixture, el, cards, button, response, confirm, appointments, router };
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
});
