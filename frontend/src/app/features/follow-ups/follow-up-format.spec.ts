import { answerCountLabel, dateChip, followUpCountLabel, formatNumber, parseDecimal } from './follow-up-format';

describe('follow-up format', () => {
  it('reads and writes decimals with a comma', () => {
    expect(parseDecimal('1,5')).toBe(1.5);
    expect(parseDecimal(' 14 ')).toBe(14);
    expect(parseDecimal('')).toBeNull();
    expect(parseDecimal('1,5,2')).toBeNull();
    expect(formatNumber(1.5)).toBe('1,5');
  });

  it('builds the date chip without timezone shifts', () => {
    expect(dateChip('2026-10-28')).toEqual({ day: '28', month: 'OCT' });
    expect(dateChip('2026-01-01').day).toBe('1');
  });

  it('pluralizes the counts', () => {
    expect(followUpCountLabel(1)).toBe('1 seguimiento');
    expect(followUpCountLabel(0)).toBe('0 seguimientos');
    expect(answerCountLabel(3)).toBe('3 respuestas');
  });
});
