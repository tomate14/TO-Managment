import { userInitials } from './user-initials';

describe('userInitials', () => {
  it('uses the first letter of the first two words', () => {
    expect(userInitials('Mariana Gómez')).toBe('MG');
    expect(userInitials('ana maría pérez')).toBe('AM');
  });

  it('handles a single word and extra spaces', () => {
    expect(userInitials('  Ana  ')).toBe('A');
  });

  it('returns an empty string for blank names', () => {
    expect(userInitials('   ')).toBe('');
  });
});
