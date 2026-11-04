/** True when the token's exp claim is not in the future. Unreadable tokens count as expired. */
export function isJwtExpired(token: string, now: Date = new Date()): boolean {
  try {
    const payload = token.split('.')[1];
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    const exp = (JSON.parse(json) as { exp?: unknown }).exp;
    return typeof exp !== 'number' || exp * 1000 <= now.getTime();
  } catch {
    return true;
  }
}
