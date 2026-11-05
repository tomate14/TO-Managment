import { vi } from 'vitest';
import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { UserSummary } from '../../../core/auth/auth.models';
import { DiagnosesService } from '../../diagnoses/diagnoses.service';
import { Patient } from '../../patients/patients.models';
import { PatientsService } from '../../patients/patients.service';
import { appointmentOf } from '../agenda-test-data';
import { AgendaTherapist, Appointment, Slot } from '../agenda.models';
import { AgendaService } from '../agenda.service';
import { AppointmentForm } from './appointment-form';

describe('AppointmentForm', () => {
  const therapists: AgendaTherapist[] = [
    { id: 2, fullName: 'Carla Ríos', color: 2, active: true },
    { id: 3, fullName: 'Paula Vidal', color: 3, active: true },
    { id: 4, fullName: 'Sofía Núñez', color: 4, active: false },
  ];
  const slots: Slot[] = [
    { startTime: '09:00:00', available: false, reason: 'THERAPIST_BUSY' },
    { startTime: '12:00:00', available: true, reason: null },
    { startTime: '12:30:00', available: true, reason: null },
  ];
  const patient = { id: 7, firstName: 'Lucía', lastName: 'Fernández', dni: '30123456' } as Patient;
  const therapistUser: UserSummary = { id: 2, email: 't@c.com', fullName: 'Carla Ríos', role: 'TERAPEUTA' };

  let availability: ReturnType<typeof vi.fn>;
  let book: ReturnType<typeof vi.fn>;
  let reschedule: ReturnType<typeof vi.fn>;

  function setup(options: { mode?: 'new' | 'reschedule'; appointment?: Appointment; user?: UserSummary } = {}) {
    availability = vi.fn().mockReturnValue(of(slots));
    book = vi.fn().mockReturnValue(of(appointmentOf({ id: 99 })));
    reschedule = vi.fn().mockReturnValue(of(appointmentOf({ id: 5 })));
    TestBed.configureTestingModule({
      providers: [
        { provide: AgendaService, useValue: { availability, book, reschedule } },
        { provide: PatientsService, useValue: { search: vi.fn().mockReturnValue(of({ content: [patient] })) } },
        { provide: DiagnosesService, useValue: { listForPatient: vi.fn().mockReturnValue(of([])) } },
      ],
    });
    const fixture = TestBed.createComponent(AppointmentForm);
    fixture.componentRef.setInput('mode', options.mode ?? 'new');
    fixture.componentRef.setInput('appointment', options.appointment ?? null);
    fixture.componentRef.setInput('therapists', therapists);
    fixture.componentRef.setInput('currentUser', options.user ?? therapistUser);
    fixture.componentRef.setInput('today', '2026-11-05');
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  const click = (fixture: { detectChanges(): void }, el: HTMLElement, selector: string) => {
    el.querySelector<HTMLElement>(selector)!.click();
    fixture.detectChanges();
  };

  it('fixes the therapist for a therapist user', () => {
    const { el } = setup();
    expect((el.querySelector('#appointment-therapist') as HTMLSelectElement).disabled).toBe(true);
  });

  it('asks availability with the current choices and disables busy slots', () => {
    const { fixture, el } = setup();
    fixture.componentInstance['pickPatient'](patient);
    fixture.detectChanges();
    expect(availability).toHaveBeenCalledWith({ therapistId: 2, patientId: 7, date: '2026-11-05', durationMinutes: 45 });
    expect((el.querySelector('button[aria-label="09:00, no disponible"]') as HTMLButtonElement).disabled).toBe(true);
  });

  it('books and emits the saved appointment', () => {
    const { fixture, el } = setup();
    const saved: Appointment[] = [];
    fixture.componentInstance.saved.subscribe((appointment) => saved.push(appointment));
    fixture.componentInstance['pickPatient'](patient);
    fixture.detectChanges();
    click(fixture, el, 'button[aria-label="12:00"]');
    expect(el.querySelector('.booking__summary')?.textContent).toContain('Jueves 5 de noviembre · 12:00 a 12:45');
    expect(el.querySelector('.booking__summary')?.textContent).toContain('Lucía Fernández con Carla Ríos');
    click(fixture, el, 'button[type="submit"]');
    expect(book).toHaveBeenCalledWith({
      therapistId: 2,
      patientId: 7,
      diagnosisId: null,
      date: '2026-11-05',
      startTime: '12:00',
      durationMinutes: 45,
      notes: null,
    });
    expect(saved).toHaveLength(1);
  });

  it('refreshes slots when the slot was taken meanwhile', () => {
    const { fixture, el } = setup();
    book.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 409, error: { code: 'THERAPIST_BUSY', message: 'Ese horario ya no está disponible.' } })),
    );
    fixture.componentInstance['pickPatient'](patient);
    fixture.detectChanges();
    click(fixture, el, 'button[aria-label="12:00"]');
    const calls = availability.mock.calls.length;
    click(fixture, el, 'button[type="submit"]');
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Ese horario ya no está disponible.');
    expect(availability.mock.calls.length).toBe(calls + 1);
  });

  it('keeps the patient locked when rescheduling', () => {
    const { fixture, el } = setup({
      mode: 'reschedule',
      appointment: appointmentOf({ id: 5, therapist: { id: 2, fullName: 'Carla Ríos', color: 2 } }),
    });
    expect(el.querySelector('#appointment-patient-search')).toBeNull();
    expect(el.querySelector('.booking__locked')?.textContent).toContain('Lucía Fernández');
    expect(availability).toHaveBeenCalledWith({
      therapistId: 2,
      patientId: 10,
      date: '2026-11-05',
      durationMinutes: 45,
      excludeAppointmentId: 5,
    });
    click(fixture, el, 'button[aria-label="12:00"]');
    click(fixture, el, 'button[type="submit"]');
    expect(reschedule).toHaveBeenCalledWith(5, expect.objectContaining({ startTime: '12:00', therapistId: 2 }));
  });

  it('shows an alert when availability fails', () => {
    const { fixture, el } = setup();
    availability.mockReturnValue(throwError(() => new Error('x')));
    fixture.componentInstance['pickPatient'](patient);
    fixture.detectChanges();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('No pudimos cargar los horarios. Intentá de nuevo.');
  });

  it('shows a hint when the day has no slots', () => {
    const { fixture, el } = setup();
    availability.mockReturnValue(of([]));
    fixture.componentInstance['pickPatient'](patient);
    fixture.detectChanges();
    expect(el.textContent).toContain('No hay horarios disponibles para este día.');
    expect(el.querySelector('[role="alert"]')).toBeNull();
  });

  it('lets an admin choose among active therapists only', () => {
    const { el } = setup({ user: { id: 1, email: 'a@c.com', fullName: 'Ana', role: 'ADMIN' } });
    const names = Array.from(el.querySelectorAll('#appointment-therapist option')).map((option) => option.textContent?.trim());
    expect(names).toEqual(['Carla Ríos', 'Paula Vidal']);
  });
});
