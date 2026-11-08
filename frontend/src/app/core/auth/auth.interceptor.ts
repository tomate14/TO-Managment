import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService, LOGIN_URL, REGISTER_URL } from './auth.service';
import { apiErrorCode } from '../api/api-error';

const API_PREFIX = '/api/';

/** Adds the bearer token to API calls and ends the session on 401 (expired → login with notice). */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const isPublic = req.url === LOGIN_URL || req.url === REGISTER_URL;
  const sendsToken = token !== null && req.url.startsWith(API_PREFIX) && !isPublic;
  const request = sendsToken ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(request).pipe(
    catchError((error: unknown) => {
      if (sendsToken && error instanceof HttpErrorResponse && error.status === 401) {
        auth.endSession(apiErrorCode(error) === 'TOKEN_EXPIRED' ? 'expired' : 'invalid');
      }
      return throwError(() => error);
    }),
  );
};
