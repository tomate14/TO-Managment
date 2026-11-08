const AMOUNT_FORMAT = new Intl.NumberFormat('es-AR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
  useGrouping: true,
});
const LONG_DATE_FORMAT = new Intl.DateTimeFormat('es-AR', { day: 'numeric', month: 'long', year: 'numeric', timeZone: 'UTC' });

/** Same limit as the backend (`@Digits(integer = 10, fraction = 2)`). */
const MAX_INTEGER_DIGITS = 10;
const MAX_FRACTION_DIGITS = 2;

const PLAIN = /^\d+$/;
const COMMA_DECIMAL = /^(\d+|\d{1,3}(\.\d{3})+)(,\d+)?$/;
const DOT_THOUSANDS = /^\d{1,3}(\.\d{3})+$/;
const DOT_DECIMAL = /^\d+\.\d+$/;

/** 25000 → "$ 25.000,00". */
export function formatAmount(amount: number): string {
  return `$ ${AMOUNT_FORMAT.format(amount)}`;
}

/** "2026-11-02" → "2 de noviembre de 2026" (calendar date: no timezone shift). */
export function formatLongDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-').map(Number);
  return LONG_DATE_FORMAT.format(new Date(Date.UTC(year, month - 1, day)));
}

/**
 * Typed amount as a canonical "25000.50" string, or null when it is not a non-negative number.
 * Accepts "25.000,50" (dots group thousands, comma is the decimal mark), "25000,5" and "25000.5".
 * Dots alone in groups of three ("25.000") are read as thousands, as is usual for pesos. A leading "$" is ignored.
 */
function normalizeAmount(raw: string | null | undefined): string | null {
  const text = (raw ?? '').trim().replace(/^\$\s*/, '');
  if (PLAIN.test(text)) {
    return text;
  }
  if (text.includes(',')) {
    return COMMA_DECIMAL.test(text) ? text.replace(/\./g, '').replace(',', '.') : null;
  }
  if (DOT_THOUSANDS.test(text)) {
    return text.replace(/\./g, '');
  }
  return DOT_DECIMAL.test(text) ? text : null;
}

/** "25.000,50", "25000,5" or "25000.5" → the number; blank or not an amount → null. */
export function parseAmount(raw: string | null | undefined): number | null {
  const normalized = normalizeAmount(raw);
  return normalized === null ? null : Number(normalized);
}

/** Whether a parseable amount fits the backend limit: at most 10 integer digits and 2 decimals. */
export function amountFitsLimit(raw: string | null | undefined): boolean {
  const normalized = normalizeAmount(raw);
  if (normalized === null) {
    return false;
  }
  const [integer, fraction = ''] = normalized.split('.');
  return integer.replace(/^0+(?=\d)/, '').length <= MAX_INTEGER_DIGITS && fraction.length <= MAX_FRACTION_DIGITS;
}

/** "1 sesión" / "5 sesiones". */
export function sessionCountLabel(count: number): string {
  return `${count} ${count === 1 ? 'sesión' : 'sesiones'}`;
}
