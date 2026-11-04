export type Role = 'ADMIN' | 'TERAPEUTA';

export interface UserSummary {
  id: number;
  email: string;
  fullName: string;
  role: Role;
}

export interface LoginResponse {
  token: string;
  expiresAt: string;
  user: UserSummary;
}

export type SessionEndReason = 'expired' | 'invalid' | 'logout';

/** Query params of /login that make it show the "Tu sesión venció" notice. */
export const SESSION_EXPIRED_QUERY = { sesion: 'vencida' } as const;
