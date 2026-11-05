import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { appointmentOf } from '../agenda-test-data';
import { Appointment } from '../agenda.models';
import { AppointmentsList } from './appointments-list';

describe('AppointmentsList', () => {
  const appointments: Appointment[] = [
    appointmentOf({ id: 1, startTime: '09:00:00', endTime: '09:45:00', diagnosis: { id: 1, description: 'Epicondilitis' } }),
    appointmentOf({ id: 2, startTime: '10:00:00', endTime: '10:45:00' }),
    appointmentOf({ id: 3, date: '2026-11-06' }),
  ];

  function setup() {
    TestBed.configureTestingModule({ imports: [AppointmentsList], providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(AppointmentsList);
    fixture.componentRef.setInput('appointments', appointments);
    fixture.componentRef.setInput('today', '2026-11-05');
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  it('groups by day with counts and keeps only today when "Hoy" is chosen', () => {
    const { fixture, el } = setup();
    expect(el.textContent).toContain('Hoy · jueves 5 de noviembre · 2 turnos');
    expect(el.textContent).toContain('Mañana · viernes 6 de noviembre · 1 turno');
    (el.querySelector('button[data-range="hoy"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(el.textContent).not.toContain('Mañana');
  });

  it('does not list past days in "Esta semana"', () => {
    const { fixture, el } = setup();
    fixture.componentRef.setInput('appointments', [...appointments, appointmentOf({ id: 9, date: '2026-11-03' })]);
    fixture.detectChanges();
    expect(el.textContent).not.toContain('martes 3 de noviembre');
    expect(el.textContent).toContain('Hoy · jueves 5 de noviembre');
  });

  it('shows the empty state', () => {
    const { fixture, el } = setup();
    fixture.componentRef.setInput('appointments', []);
    fixture.detectChanges();
    expect(el.textContent).toContain('No hay turnos en este período.');
  });

  it('asks before cancelling and then emits', () => {
    const { fixture, el } = setup();
    const emitted: Appointment[] = [];
    fixture.componentInstance.cancel.subscribe((appointment) => emitted.push(appointment));
    (el.querySelector('button[aria-label="Cancelar turno"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(el.textContent).toContain('¿Cancelar el turno de Lucía Fernández?');
    expect(emitted).toHaveLength(0);
    (el.querySelector('button[data-action="confirm"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(emitted).toHaveLength(1);
    expect(emitted[0].id).toBe(1);
  });

  it('lets the user back out of the confirmation', () => {
    const { fixture, el } = setup();
    (el.querySelector('button[aria-label="Cancelar turno"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    (el.querySelector('button[data-action="keep"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(el.textContent).not.toContain('¿Cancelar el turno');
  });

  it('links to the patient page and shows the times and diagnosis without status badges', () => {
    const { el } = setup();
    expect(el.querySelector('a.appointment-card__patient-link')!.getAttribute('href')).toBe('/pacientes/10');
    expect(el.textContent).toContain('09:00');
    expect(el.textContent).toContain('a 09:45');
    expect(el.textContent).toContain('Epicondilitis');
    expect(el.textContent).not.toContain('Confirmado');
  });
});
