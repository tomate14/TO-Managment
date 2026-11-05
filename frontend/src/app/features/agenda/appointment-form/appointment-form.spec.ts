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

  let search: ReturnType<typeof vi.fn>;
  let create: ReturnType<typeof vi.fn>;

  function setup(options: { mode?: 'new' | 'reschedule'; appointment?: Appointment; user?: UserSummary } = {}) {
    search = vi.fn().mockReturnValue(of({ content: [patient] }));
    create = vi.fn().mockReturnValue(of(patient));
    availability = vi.fn().mockReturnValue(of(slots));
    book = vi.fn().mockReturnValue(of(appointmentOf({ id: 99 })));
    reschedule = vi.fn().mockReturnValue(of(appointmentOf({ id: 5 })));
    TestBed.configureTestingModule({
      providers: [
        { provide: AgendaService, useValue: { availability, book, reschedule } },
        { provide: PatientsService, useValue: { search, create } },
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
    expect(availability).toHaveBeenCalledWith({ therapistId: 2, date: '2026-11-05', durationMinutes: 45 });
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

  describe('availability requests', () => {
    const typeSearch = (fixture: { detectChanges(): void }, el: HTMLElement, text: string) => {
      const input = el.querySelector<HTMLInputElement>('#appointment-patient-search')!;
      input.value = text;
      input.dispatchEvent(new Event('input'));
      vi.advanceTimersByTime(300);
      fixture.detectChanges();
    };
    beforeEach(() => vi.useFakeTimers());
    afterEach(() => vi.useRealTimers());

    it('is not requested again when typing in the patient search or the notes', () => {
      const { fixture, el } = setup();
      const calls = availability.mock.calls.length;
      typeSearch(fixture, el, 'Luc');
      typeSearch(fixture, el, 'Lucía');
      const notes = el.querySelector<HTMLTextAreaElement>('#appointment-notes')!;
      notes.value = 'Traer la férula';
      notes.dispatchEvent(new Event('input'));
      fixture.detectChanges();
      expect(availability.mock.calls.length).toBe(calls);
    });

    it('is requested again, without the patient, when the duration changes', () => {
      const { fixture, el } = setup();
      const calls = availability.mock.calls.length;
      const duration = el.querySelector<HTMLSelectElement>('#appointment-duration')!;
      duration.selectedIndex = duration.selectedIndex === 0 ? 1 : 0;
      duration.dispatchEvent(new Event('change'));
      fixture.detectChanges();
      expect(availability.mock.calls.length).toBe(calls + 1);
      expect(availability.mock.lastCall![0]).not.toHaveProperty('patientId');
    });

    it('is requested again when the date changes', () => {
      const { fixture, el } = setup();
      const calls = availability.mock.calls.length;
      const date = el.querySelector<HTMLInputElement>('#appointment-date')!;
      date.value = '2026-11-06';
      date.dispatchEvent(new Event('input'));
      fixture.detectChanges();
      expect(availability.mock.calls.length).toBe(calls + 1);
      expect(availability).toHaveBeenLastCalledWith({ therapistId: 2, date: '2026-11-06', durationMinutes: 45 });
    });
  });

  describe('resolving the patient on submit', () => {
    const typeSearch = (fixture: { detectChanges(): void }, el: HTMLElement, text: string) => {
      const input = el.querySelector<HTMLInputElement>('#appointment-patient-search')!;
      input.value = text;
      input.dispatchEvent(new Event('input'));
      vi.advanceTimersByTime(300);
      fixture.detectChanges();
    };
    const fillPatientForm = (el: HTMLElement) => {
      const values: Record<string, string> = {
        '#patient-first-name': 'Lucía',
        '#patient-last-name': 'Fernández',
        '#patient-birth-date': '1984-03-21',
        '#patient-phone': '249 412-3456',
        '#patient-occupation': 'Docente',
        '#patient-insurance': 'OSDE',
        '#patient-insurance-number': '61 234567 8 01',
      };
      for (const [selector, value] of Object.entries(values)) {
        const input = el.querySelector<HTMLInputElement>(selector)!;
        input.value = value;
        input.dispatchEvent(new Event('input'));
      }
    };
    const buttonByText = (el: HTMLElement, text: string) =>
      Array.from(el.querySelectorAll('button')).find((button) => button.textContent?.trim() === text);

    beforeEach(() => vi.useFakeTimers());
    afterEach(() => vi.useRealTimers());

    it('no longer offers to register from the search results', () => {
      const { fixture, el } = setup();
      search.mockReturnValue(of({ content: [] }));
      typeSearch(fixture, el, '30123456');
      expect(el.textContent).toContain('No encontramos pacientes con esa búsqueda.');
      expect(buttonByText(el, 'Dar de alta')).toBeUndefined();
    });

    it('books with the picked patient', () => {
      const { fixture, el } = setup();
      fixture.componentInstance['pickPatient'](patient);
      fixture.detectChanges();
      click(fixture, el, 'button[aria-label="12:00"]');
      click(fixture, el, 'button[type="submit"]');
      expect(book).toHaveBeenCalledWith(expect.objectContaining({ patientId: 7 }));
    });

    it('resolves a typed existing DNI and books with its id', () => {
      const { fixture, el } = setup();
      typeSearch(fixture, el, '30.123.456');
      search.mockClear();
      click(fixture, el, 'button[aria-label="12:00"]');
      click(fixture, el, 'button[type="submit"]');
      expect(search).toHaveBeenCalledWith('30123456', 0, expect.any(Number));
      expect(book).toHaveBeenCalledWith(expect.objectContaining({ patientId: 7, startTime: '12:00' }));
    });

    it('opens the registration with the DNI prefilled for an unknown DNI, then books after saving', () => {
      const { fixture, el } = setup();
      const notes = el.querySelector<HTMLTextAreaElement>('#appointment-notes')!;
      notes.value = 'Traer la férula';
      notes.dispatchEvent(new Event('input'));
      const duration = el.querySelector<HTMLSelectElement>('#appointment-duration')!;
      duration.selectedIndex = 1;
      duration.dispatchEvent(new Event('change'));
      const chosenDuration = fixture.componentInstance.form.controls.durationMinutes.value;
      fixture.detectChanges();
      typeSearch(fixture, el, '30123456');
      search.mockReturnValue(of({ content: [] }));
      click(fixture, el, 'button[aria-label="12:00"]');
      click(fixture, el, 'button[type="submit"]');

      expect(book).not.toHaveBeenCalled();
      expect(el.querySelector('.register__title')?.textContent).toContain('Nuevo paciente');
      expect(el.querySelector<HTMLInputElement>('#patient-dni')?.value).toBe('30123456');
      expect(buttonByText(el, 'Volver al turno')).toBeDefined();

      fillPatientForm(el);
      el.querySelector('app-patient-form form')!.dispatchEvent(new Event('submit'));
      fixture.detectChanges();

      expect(create).toHaveBeenCalledWith(expect.objectContaining({ dni: '30123456' }));
      expect(book).toHaveBeenCalledWith({
        therapistId: 2,
        patientId: 7,
        diagnosisId: null,
        date: '2026-11-05',
        startTime: '12:00',
        durationMinutes: chosenDuration,
        notes: 'Traer la férula',
      });
    });

    it('returns to the booking without changes when the registration is cancelled', () => {
      const { fixture, el } = setup();
      typeSearch(fixture, el, '30123456');
      search.mockReturnValue(of({ content: [] }));
      click(fixture, el, 'button[aria-label="12:00"]');
      click(fixture, el, 'button[type="submit"]');
      buttonByText(el, 'Volver al turno')!.click();
      fixture.detectChanges();
      expect(el.querySelector('app-patient-form')).toBeNull();
      expect(el.querySelector<HTMLInputElement>('#appointment-patient-search')?.value).toBe('30123456');
      expect(create).not.toHaveBeenCalled();
      expect(book).not.toHaveBeenCalled();
    });

    it('asks for a patient or DNI and does not book when there is neither', () => {
      const { fixture, el } = setup();
      click(fixture, el, 'button[aria-label="12:00"]');
      click(fixture, el, 'button[type="submit"]');
      expect(el.querySelector('#appointment-patient-error')?.textContent).toContain('Elegí un paciente o ingresá su DNI.');
      expect(book).not.toHaveBeenCalled();
      expect(search).not.toHaveBeenCalled();
    });
  });

  it('lets an admin choose among active therapists only', () => {
    const { el } = setup({ user: { id: 1, email: 'a@c.com', fullName: 'Ana', role: 'ADMIN' } });
    const names = Array.from(el.querySelectorAll('#appointment-therapist option')).map((option) => option.textContent?.trim());
    expect(names).toEqual(['Carla Ríos', 'Paula Vidal']);
  });
});
