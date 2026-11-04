import {
  ageInYears, dominanceLabel, formatDate, formatDni, fullName, listName, normalizeDni, patientInitials, telHref, todayIso,
} from './patient-format';

describe('patient format helpers', () => {
  const lucia = { firstName: 'Lucía', lastName: 'Fernández' };

  it('formats and normalizes DNIs', () => {
    expect(formatDni('30123456')).toBe('30.123.456');
    expect(formatDni('7123456')).toBe('7.123.456');
    expect(normalizeDni(' 30.123 456 ')).toBe('30123456');
  });

  it('builds names and initials', () => {
    expect(listName(lucia)).toBe('Fernández, Lucía');
    expect(fullName(lucia)).toBe('Lucía Fernández');
    expect(patientInitials(lucia)).toBe('LF');
  });

  it('labels dominance as in the spec', () => {
    expect(dominanceLabel('RIGHT')).toBe('Diestro');
    expect(dominanceLabel('LEFT')).toBe('Zurdo');
  });

  it('formats dates without timezone shifts and computes the age', () => {
    expect(formatDate('1984-03-21')).toBe('21/03/1984');
    expect(ageInYears('1984-03-21', new Date(2026, 10, 4))).toBe(42);
    expect(ageInYears('1984-11-05', new Date(2026, 10, 4))).toBe(41);
    expect(ageInYears('1984-11-04', new Date(2026, 10, 4))).toBe(42);
    expect(todayIso(new Date(2026, 0, 5))).toBe('2026-01-05');
  });

  it('builds a dialable tel: link', () => {
    expect(telHref('249 412-3456')).toBe('tel:2494123456');
  });
});
