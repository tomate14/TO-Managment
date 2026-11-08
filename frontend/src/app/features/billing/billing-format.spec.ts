import { amountFitsLimit, formatAmount, formatLongDate, parseAmount, sessionCountLabel } from './billing-format';

describe('billing-format', () => {
  it('formats amounts in es-AR with "$" and two decimals', () => {
    expect(formatAmount(25000)).toBe('$ 25.000,00');
    expect(formatAmount(1500)).toBe('$ 1.500,00');
    expect(formatAmount(15000.5)).toBe('$ 15.000,50');
    expect(formatAmount(0.75)).toBe('$ 0,75');
  });

  it('formats long dates in Spanish without a timezone shift', () => {
    expect(formatLongDate('2026-11-02')).toBe('2 de noviembre de 2026');
    expect(formatLongDate('2026-09-30')).toBe('30 de septiembre de 2026');
    expect(formatLongDate('2026-01-01')).toBe('1 de enero de 2026');
  });

  it('parses amounts with comma or dot decimals and dot thousands', () => {
    expect(parseAmount('25.000,50')).toBe(25000.5);
    expect(parseAmount('25000,5')).toBe(25000.5);
    expect(parseAmount('25000.5')).toBe(25000.5);
    expect(parseAmount('25000')).toBe(25000);
    expect(parseAmount('25.000')).toBe(25000);
    expect(parseAmount('1.250.000,00')).toBe(1250000);
    expect(parseAmount(' $ 20.000,00 ')).toBe(20000);
    expect(parseAmount('0')).toBe(0);
  });

  it('returns null for blank or invalid amounts', () => {
    expect(parseAmount('')).toBeNull();
    expect(parseAmount('   ')).toBeNull();
    expect(parseAmount(null)).toBeNull();
    expect(parseAmount('abc')).toBeNull();
    expect(parseAmount('-5')).toBeNull();
    expect(parseAmount('25,000,50')).toBeNull();
    expect(parseAmount('2.50.0')).toBeNull();
    expect(parseAmount('25.00,5')).toBeNull();
  });

  it('checks the backend digit limit: 10 integer digits and 2 decimals', () => {
    expect(amountFitsLimit('9999999999,99')).toBe(true);
    expect(amountFitsLimit('25.000,50')).toBe(true);
    expect(amountFitsLimit('10000000000')).toBe(false);
    expect(amountFitsLimit('25000,505')).toBe(false);
    expect(amountFitsLimit('abc')).toBe(false);
  });

  it('labels session counts in singular and plural', () => {
    expect(sessionCountLabel(1)).toBe('1 sesión');
    expect(sessionCountLabel(5)).toBe('5 sesiones');
  });
});
