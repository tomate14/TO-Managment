import { Role } from '../../core/auth/auth.models';

/** A user as returned by /api/users (admin screens). */
export interface ManagedUser {
  id: number;
  email: string;
  fullName: string;
  role: Role;
  active: boolean;
}

export interface CreateUserPayload {
  email: string;
  fullName: string;
  password: string;
  role: Role;
}

export interface UpdateUserPayload {
  fullName: string;
  role: Role;
}

/** Backend error codes these screens react to. */
export const USER_ERROR_CODES = {
  emailTaken: 'USER_EMAIL_TAKEN',
} as const;
