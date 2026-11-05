import { userInitials } from '../../layout/user-initials';
import { DOMINANCE_OPTIONS, Dominance, Patient } from './patients.models';

/** "30123456" → "30.123.456". */
export function formatDni(dni: string): string {
  return dni.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
}

/** A DNI is 6 to 9 digits (same rule as the backend); expects the output of `normalizeDni`. */
export function isValidDni(digits: string): boolean {
  return /^\d{6,9}$/.test(digits);
}

/** Keeps only digits, as the backend stores the DNI. */
export function normalizeDni(raw: string): string {
  return raw.replace(/[\s.-]/g, '');
}

/** "Fernández, Lucía" (list order). */
export function listName(patient: Pick<Patient, 'firstName' | 'lastName'>): string {
  return `${patient.lastName}, ${patient.firstName}`;
}

export function fullName(patient: Pick<Patient, 'firstName' | 'lastName'>): string {
  return `${patient.firstName} ${patient.lastName}`;
}

export function patientInitials(patient: Pick<Patient, 'firstName' | 'lastName'>): string {
  return userInitials(fullName(patient));
}

export function dominanceLabel(dominance: Dominance): string {
  return DOMINANCE_OPTIONS.find((option) => option.value === dominance)?.label ?? dominance;
}

/** "1984-03-21" → "21/03/1984" (no timezone conversion: it is a calendar date). */
export function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-');
  return `${day}/${month}/${year}`;
}

/** Whole years between the birth date and `today`. */
export function ageInYears(isoBirthDate: string, today: Date = new Date()): number {
  const [year, month, day] = isoBirthDate.split('-').map(Number);
  let age = today.getFullYear() - year;
  const beforeBirthday = today.getMonth() + 1 < month || (today.getMonth() + 1 === month && today.getDate() < day);
  if (beforeBirthday) {
    age -= 1;
  }
  return age;
}

/** Local calendar date as yyyy-mm-dd, for the max of date inputs. */
export function todayIso(today: Date = new Date()): string {
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;
}

/** "249 412-3456" → "tel:2494123456". */
export function telHref(phone: string): string {
  return `tel:${phone.replace(/[^\d+]/g, '')}`;
}
