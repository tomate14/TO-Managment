import { isJwtExpired } from './jwt-expiry';

function jwtWithExp(expSeconds: number): string {
  const payload = btoa(JSON.stringify({ sub: '1', exp: expSeconds })).replace(/=+$/, '');
  return `header.${payload}.signature`;
}

describe('isJwtExpired', () => {
  const now = new Date('2026-11-04T12:00:00Z');

  it('returns false before exp', () => {
    expect(isJwtExpired(jwtWithExp(now.getTime() / 1000 + 60), now)).toBe(false);
  });

  it('returns true at or after exp', () => {
    expect(isJwtExpired(jwtWithExp(now.getTime() / 1000), now)).toBe(true);
  });

  it('treats malformed tokens as expired', () => {
    expect(isJwtExpired('not-a-jwt', now)).toBe(true);
    expect(isJwtExpired('a.%%%.c', now)).toBe(true);
  });
});
