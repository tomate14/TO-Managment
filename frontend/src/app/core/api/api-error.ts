import { HttpErrorResponse } from '@angular/common/http';

/** Backend ErrorResponse `code` (e.g. AUTHENTICATION_FAILED), or null when the error is not a backend error body. */
export function apiErrorCode(error: unknown): string | null {
  if (error instanceof HttpErrorResponse && error.error && typeof error.error.code === 'string') {
    return error.error.code;
  }
  return null;
}

/** User-facing (Spanish) message of a backend ErrorResponse, or null when there is none. */
export function apiErrorMessage(error: unknown): string | null {
  if (error instanceof HttpErrorResponse && error.error && typeof error.error.message === 'string') {
    return error.error.message;
  }
  return null;
}
