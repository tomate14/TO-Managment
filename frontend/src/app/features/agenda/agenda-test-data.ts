import { Appointment } from './agenda.models';

/** Spec helper: builds an appointment with sensible defaults. */
export function appointmentOf(overrides: Partial<Appointment> & { id: number }): Appointment {
  return {
    therapist: { id: 1, fullName: 'Mariana Gómez', color: 1 },
    patient: { id: 10, firstName: 'Lucía', lastName: 'Fernández', dni: '30111222' },
    diagnosis: null,
    date: '2026-11-05',
    startTime: '09:00:00',
    endTime: '09:45:00',
    durationMinutes: 45,
    notes: null,
    confirmedAt: null,
    billed: false,
    ...overrides,
  };
}
