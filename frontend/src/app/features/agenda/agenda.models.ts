export interface AgendaTherapist {
  id: number;
  fullName: string;
  color: number;
  active: boolean;
}

/**
 * The API serializes `startTime`/`endTime` as "HH:mm:ss" (Java LocalTime). The frontend only
 * cares about "HH:mm": parse and display times by their first 5 characters.
 */
export interface Appointment {
  id: number;
  therapist: { id: number; fullName: string; color: number };
  patient: { id: number; firstName: string; lastName: string; dni: string };
  diagnosis: { id: number; description: string } | null;
  date: string;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  notes: string | null;
  confirmedAt: string | null;
  billed: boolean;
}

export type SlotUnavailableReason = 'PAST' | 'THERAPIST_BUSY' | 'PATIENT_BUSY';

/** `startTime` may come as "HH:mm:ss"; use its first 5 characters. */
export interface Slot {
  startTime: string;
  available: boolean;
  reason: SlotUnavailableReason | null;
}

export interface AppointmentPayload {
  therapistId: number;
  patientId?: number;
  diagnosisId: number | null;
  date: string;
  startTime: string;
  durationMinutes: number;
  notes: string | null;
}

export interface AvailabilityQuery {
  therapistId: number;
  patientId?: number;
  date: string;
  durationMinutes: number;
  excludeAppointmentId?: number;
}

export const DURATIONS = [30, 45, 60] as const;
export const AGENDA_HOURS = { first: 8, last: 20 } as const;
