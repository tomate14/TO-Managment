export interface EstablishmentAdmin {
  id: number;
  fullName: string;
  email: string;
}

export type EstablishmentKind = 'INSTITUTION' | 'INDIVIDUAL';

/** An establishment as returned by /api/establishments (superadmin screens). */
export interface EstablishmentSummary {
  id: number;
  name: string;
  kind: EstablishmentKind;
  active: boolean;
  /** An independent practice waiting for the superadmin's approval. */
  pendingApproval: boolean;
  /** An independent practice the superadmin rejected. */
  rejected: boolean;
  /** ISO instant of the registration request. */
  createdAt: string;
  activeUsers: number;
  admins: EstablishmentAdmin[];
}

export interface CreateEstablishmentPayload {
  name: string;
  admin: {
    fullName: string;
    email: string;
    password: string;
  };
}

/** Copy shown when an establishment name is already used. */
export const ESTABLISHMENT_NAME_TAKEN_MESSAGE = 'Ya hay un establecimiento con ese nombre.';

/** Backend error codes these screens react to. */
export const ESTABLISHMENT_ERROR_CODES = {
  nameTaken: 'ESTABLISHMENT_NAME_TAKEN',
  emailTaken: 'USER_EMAIL_TAKEN',
  notFound: 'ESTABLISHMENT_NOT_FOUND',
} as const;
