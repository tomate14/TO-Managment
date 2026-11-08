export type Role = 'SUPERADMIN' | 'ADMIN' | 'TERAPEUTA';

export type EstablishmentKind = 'INSTITUTION' | 'INDIVIDUAL';

export interface EstablishmentRef {
  id: number;
  name: string;
  kind: EstablishmentKind;
}

export interface UserSummary {
  id: number;
  email: string;
  fullName: string;
  role: Role;
  /** Establishment of the session; null for a superadmin outside any establishment. */
  establishment: EstablishmentRef | null;
}

export interface LoginResponse {
  token: string;
  expiresAt: string;
  user: UserSummary;
}

export type SessionEndReason = 'expired' | 'invalid' | 'logout';

/** Query params of /login that make it show the "Tu sesión venció" notice. */
export const SESSION_EXPIRED_QUERY = { sesion: 'vencida' } as const;

/** Admin screens: an ADMIN, or a SUPERADMIN acting inside an establishment. */
export function isAdminInEstablishment(user: UserSummary | null | undefined): boolean {
  if (!user) {
    return false;
  }
  return user.role === 'ADMIN' || (user.role === 'SUPERADMIN' && !!user.establishment);
}

/** An independent therapist: the admin of her own individual practice ("Mi consultorio"). */
export function isIndividualPractice(user: UserSummary | null | undefined): boolean {
  return user?.role === 'ADMIN' && user.establishment?.kind === 'INDIVIDUAL';
}
