import { FollowUpAnswer } from './follow-ups.models';

/** A recorded answer or ROM metric value: one typed value according to its answer type. */
export type RecordedValue = Pick<FollowUpAnswer, 'answerType' | 'numericValue' | 'booleanValue' | 'optionValue' | 'textValue'>;

const NUMBER_FORMAT = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 3 });
const MONTH_FORMAT = new Intl.DateTimeFormat('es-AR', { month: 'short', timeZone: 'UTC' });

/** 1.5 → "1,5". */
export function formatNumber(value: number): string {
  return NUMBER_FORMAT.format(value);
}

/** "1,5" or "1.5" → 1.5; blank or not a number → null. */
export function parseDecimal(raw: string | null | undefined): number | null {
  const text = (raw ?? '').trim().replace(',', '.');
  if (text === '' || !/^-?\d+(\.\d+)?$/.test(text)) {
    return null;
  }
  return Number(text);
}

/** "2026-10-28" → { day: "28", month: "OCT" } for the date chip (calendar date: no timezone shift). */
export function dateChip(isoDate: string): { day: string; month: string } {
  const [year, month, day] = isoDate.split('-').map(Number);
  const monthLabel = MONTH_FORMAT.format(new Date(Date.UTC(year, month - 1, day))).replace('.', '').toUpperCase();
  return { day: String(day), month: monthLabel };
}

/** Value of an answer (or of a ROM metric) as shown in the history. */
export function formatAnswer(answer: RecordedValue): string {
  switch (answer.answerType) {
    case 'NUMERIC':
      return answer.numericValue === null ? '' : formatNumber(answer.numericValue);
    case 'YES_NO':
      return answer.booleanValue ? 'Sí' : 'No';
    case 'OPTION':
      return answer.optionValue ?? '';
    case 'TEXT':
      return answer.textValue ?? '';
  }
}

/** "1 seguimiento" / "3 seguimientos". */
export function followUpCountLabel(count: number): string {
  return `${count} ${count === 1 ? 'seguimiento' : 'seguimientos'}`;
}

/** "1 respuesta" / "3 respuestas". */
export function answerCountLabel(count: number): string {
  return `${count} ${count === 1 ? 'respuesta' : 'respuestas'}`;
}

/** "1 sesión pendiente de facturar" / "3 sesiones pendientes de facturar". */
export function pendingBillingLabel(count: number): string {
  return count === 1 ? '1 sesión pendiente de facturar' : `${count} sesiones pendientes de facturar`;
}
