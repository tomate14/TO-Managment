import { InjectionToken } from '@angular/core';
import { todayIso } from '../patients/patient-format';
import { Appointment } from './agenda.models';

/** Source of "today" (YYYY-MM-DD); specs provide a fixed date. */
export const AGENDA_TODAY = new InjectionToken<() => string>('AGENDA_TODAY', { factory: () => todayIso });

// Spanish names are explicit arrays (not Intl) so the output never depends on the ICU data of the runtime.
const MONTHS = [
  'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
  'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre',
];
const WEEKDAYS = ['domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado'];

const DAY_MS = 24 * 60 * 60 * 1000;
const WEEK_DAYS = 7;
const MONTH_GRID_DAYS = 42;

// All calendar dates are 'YYYY-MM-DD' and computed on UTC midnights: no timezone/DST shifts.
function parse(iso: string): Date {
  const [year, month, day] = iso.split('-').map(Number);
  return new Date(Date.UTC(year, month - 1, day));
}

function format(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())}`;
}

export function addDays(iso: string, days: number): string {
  return format(new Date(parse(iso).getTime() + days * DAY_MS));
}

/** Monday of the week containing `iso`. */
export function weekStart(iso: string): string {
  const mondayBased = (parse(iso).getUTCDay() + 6) % WEEK_DAYS;
  return addDays(iso, -mondayBased);
}

/** Monday..Sunday. */
export function weekDays(iso: string): string[] {
  const start = weekStart(iso);
  return Array.from({ length: WEEK_DAYS }, (_, index) => addDays(start, index));
}

/** 42 days starting on the Monday on or before the 1st of the month of `iso`. */
export function monthGrid(iso: string): string[] {
  const first = `${iso.slice(0, 7)}-01`;
  const start = weekStart(first);
  return Array.from({ length: MONTH_GRID_DAYS }, (_, index) => addDays(start, index));
}

/** "2 – 8 de noviembre de 2026" or "30 de noviembre – 6 de diciembre de 2026". */
export function weekTitle(iso: string): string {
  const days = weekDays(iso);
  const start = parse(days[0]);
  const end = parse(days[WEEK_DAYS - 1]);
  const endText = `${end.getUTCDate()} de ${MONTHS[end.getUTCMonth()]} de ${end.getUTCFullYear()}`;
  if (start.getUTCFullYear() !== end.getUTCFullYear()) {
    return `${start.getUTCDate()} de ${MONTHS[start.getUTCMonth()]} de ${start.getUTCFullYear()} – ${endText}`;
  }
  if (start.getUTCMonth() !== end.getUTCMonth()) {
    return `${start.getUTCDate()} de ${MONTHS[start.getUTCMonth()]} – ${endText}`;
  }
  return `${start.getUTCDate()} – ${endText}`;
}

/** "Hoy · jueves 5 de noviembre", "Mañana · …", otherwise "Sábado 7 de noviembre". */
export function dayHeading(iso: string, todayIso: string): string {
  const date = parse(iso);
  const text = `${WEEKDAYS[date.getUTCDay()]} ${date.getUTCDate()} de ${MONTHS[date.getUTCMonth()]}`;
  if (iso === todayIso) {
    return `Hoy · ${text}`;
  }
  if (iso === addDays(todayIso, 1)) {
    return `Mañana · ${text}`;
  }
  return text.charAt(0).toUpperCase() + text.slice(1);
}

/** "Jueves 5 de noviembre". */
export function longDate(iso: string): string {
  const date = parse(iso);
  const text = `${WEEKDAYS[date.getUTCDay()]} ${date.getUTCDate()} de ${MONTHS[date.getUTCMonth()]}`;
  return text.charAt(0).toUpperCase() + text.slice(1);
}

/** "12:00" + 45 → "12:45" (first 5 characters of `time` are used). */
export function addMinutes(time: string, minutes: number): string {
  const [hours, mins] = time.slice(0, 5).split(':').map(Number);
  const total = hours * 60 + mins + minutes;
  return `${String(Math.floor(total / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`;
}

/** "Noviembre 2026". */
export function monthTitle(iso: string): string {
  const [year, month] = iso.split('-').map(Number);
  const name = MONTHS[month - 1];
  return `${name.charAt(0).toUpperCase()}${name.slice(1)} ${year}`;
}

/** The 1st of the month `delta` months away from the month of `iso`. */
export function shiftMonth(iso: string, delta: number): string {
  const [year, month] = iso.split('-').map(Number);
  return format(new Date(Date.UTC(year, month - 1 + delta, 1)));
}

/** Groups by date, days ascending, appointments by start time. */
export function groupByDay(appointments: Appointment[]): { date: string; appointments: Appointment[] }[] {
  const byDate = new Map<string, Appointment[]>();
  for (const appointment of appointments) {
    const group = byDate.get(appointment.date) ?? [];
    group.push(appointment);
    byDate.set(appointment.date, group);
  }
  return [...byDate.keys()]
    .sort()
    .map((date) => ({
      date,
      appointments: byDate.get(date)!.sort((a, b) => a.startTime.localeCompare(b.startTime)),
    }));
}
