export type BillingSessionOrigin = 'APPOINTMENT' | 'LEGACY_FOLLOW_UP';

interface BillingSessionDetails {
  /** Local yyyy-mm-dd, never parsed as UTC. */
  date: string;
  diagnosis: { id: number; description: string } | null;
  segments: string[];
}

export interface AppointmentSessionRef extends BillingSessionDetails {
  origin: 'APPOINTMENT';
  appointmentId: number;
  followUpId: null;
  startTime: string | null;
  confirmedAt: string | null;
}

export interface LegacyFollowUpSessionRef extends BillingSessionDetails {
  origin: 'LEGACY_FOLLOW_UP';
  appointmentId: null;
  followUpId: number;
  startTime: null;
  confirmedAt: null;
}

export type BilledSessionRef = AppointmentSessionRef | LegacyFollowUpSessionRef;

export interface Billing {
  id: number;
  origin: BillingSessionOrigin;
  /** yyyy-mm-dd */
  billedOn: string;
  amount: number;
  sessionCount: number;
  voided: boolean;
  voidedAt: string | null;
  createdAt: string;
  sessions: BilledSessionRef[];
}

/** Billing state of a patient: counters, pending sessions (oldest first) and billings (newest first). */
export interface BillingSummary {
  totalSessions: number;
  billedSessions: number;
  pendingSessions: number;
  pending: BilledSessionRef[];
  billings: Billing[];
}

export interface CreateBillingPayload {
  /** yyyy-mm-dd */
  billedOn: string;
  amount: number;
  appointmentIds: number[];
}

/** Backend error codes the billing screens react to. */
export const BILLING_ERROR_CODES = {
  sessionsRequired: 'BILLING_SESSIONS_REQUIRED',
  amountInvalid: 'BILLING_AMOUNT_INVALID',
  dateInFuture: 'BILLING_DATE_IN_FUTURE',
  sessionInvalid: 'BILLING_SESSION_INVALID',
  notFound: 'BILLING_NOT_FOUND',
} as const;
