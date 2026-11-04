import { HttpErrorResponse } from '@angular/common/http';

/** Backend ErrorResponse `code` (e.g. AUTHENTICATION_FAILED), or null when the error is not a backend error body. */
export function apiErrorCode(error: unknown): string | null {
  if (error instanceof HttpErrorResponse && error.error && typeof error.error.code === 'string') {
    return error.error.code;
  }
  return null;
}
