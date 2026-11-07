import { trialLabel, trialLastDay } from './trial-format';

describe('trialLastDay', () => {
  it('is the day before the stored end (the account blocks at that midnight)', () => {
    const end = new Date(2026, 10, 15, 0, 0, 0).toISOString(); // local midnight starting 15/11
    expect(trialLastDay(end)).toBe('2026-11-14');
  });
});

describe('trialLabel', () => {
  const end = new Date(2026, 10, 15, 0, 0, 0).toISOString();

  it('shows the last day of a running trial', () => {
    expect(trialLabel({ trialEndsAt: end, trialExpired: false })).toBe('Prueba hasta el 14/11/2026');
  });

  it('shows the last day of an expired trial', () => {
    expect(trialLabel({ trialEndsAt: end, trialExpired: true })).toBe('Venció el 14/11/2026');
  });

  it('says when there is no expiry', () => {
    expect(trialLabel({ trialEndsAt: null, trialExpired: false })).toBe('Sin vencimiento');
  });
});
