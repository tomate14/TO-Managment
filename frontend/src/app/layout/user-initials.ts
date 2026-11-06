import { Role } from '../core/auth/auth.models';

export function userInitials(fullName: string): string {
  return fullName
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((word) => word[0].toUpperCase())
    .join('');
}

export const ROLE_LABELS: Record<Role, string> = {
  SUPERADMIN: 'Superadmin',
  ADMIN: 'Administrador/a',
  TERAPEUTA: 'Terapeuta',
};

/** Compact role names for badges. */
export const ROLE_SHORT_LABELS: Record<Role, string> = {
  SUPERADMIN: 'Superadmin',
  ADMIN: 'Admin',
  TERAPEUTA: 'Terapeuta',
};
