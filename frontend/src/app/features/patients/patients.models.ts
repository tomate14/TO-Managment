export type Dominance = 'LEFT' | 'RIGHT';

export interface Patient {
  id: number;
  /** Digits only; formatted with dots in the UI. */
  dni: string;
  firstName: string;
  lastName: string;
  /** ISO date (yyyy-mm-dd). */
  birthDate: string;
  phone: string;
  occupation: string;
  healthInsurance: string;
  healthInsuranceNumber: string;
  dominance: Dominance;
}

export type PatientData = Omit<Patient, 'id' | 'dni'>;

export interface CreatePatientPayload extends PatientData {
  dni: string;
}

export interface PageResponse<T> {
  content: T[];
  /** Zero-based. */
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export const PATIENT_PAGE_SIZE = 20;

export const DOMINANCE_OPTIONS: readonly { value: Dominance; label: string }[] = [
  { value: 'RIGHT', label: 'Diestro' },
  { value: 'LEFT', label: 'Zurdo' },
];

export const PATIENT_ERROR_CODES = {
  dniDuplicate: 'PATIENT_DNI_DUPLICATE',
} as const;
