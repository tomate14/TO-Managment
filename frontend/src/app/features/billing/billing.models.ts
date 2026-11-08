/** A session (follow-up) as listed inside a billing or among the pending ones. */
export interface BilledSessionRef {
  followUpId: number;
  /** yyyy-mm-dd */
  date: string;
  segments: string[];
}

export interface Billing {
  id: number;
  /** yyyy-mm-dd */
  billedOn: string;
  amount: number;
  sessionCount: number;
  voided: boolean;
  voidedAt: string | null;
  createdAt: string;
  sessions: BilledSessionRef[];
}

/** Billing state of a diagnosis: counters, pending sessions (oldest first) and billings (newest first). */
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
  followUpIds: number[];
}

/** Backend error codes the billing screens react to. */
export const BILLING_ERROR_CODES = {
  sessionsRequired: 'BILLING_SESSIONS_REQUIRED',
  amountInvalid: 'BILLING_AMOUNT_INVALID',
  dateInFuture: 'BILLING_DATE_IN_FUTURE',
  sessionInvalid: 'BILLING_SESSION_INVALID',
  notFound: 'BILLING_NOT_FOUND',
} as const;
