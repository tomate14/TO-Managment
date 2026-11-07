import { formatDate, todayIso } from '../patients/patient-format';
import { EstablishmentSummary } from './establishments.models';

/** Last usable day (local yyyy-mm-dd) of a trial whose stored end is the following midnight. */
export function trialLastDay(trialEndsAt: string): string {
  return todayIso(new Date(Date.parse(trialEndsAt) - 1));
}

/** "Venció el dd/mm/aaaa", "Prueba hasta el dd/mm/aaaa" or "Sin vencimiento" (the last day is the one shown). */
export function trialLabel(item: Pick<EstablishmentSummary, 'trialEndsAt' | 'trialExpired'>): string {
  if (item.trialEndsAt === null) {
    return 'Sin vencimiento';
  }
  const lastDay = formatDate(trialLastDay(item.trialEndsAt));
  return item.trialExpired ? `Venció el ${lastDay}` : `Prueba hasta el ${lastDay}`;
}
