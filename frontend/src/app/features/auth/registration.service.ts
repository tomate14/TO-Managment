import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export const REGISTER_URL = '/api/auth/register';

export interface RegistrationPayload {
  fullName: string;
  email: string;
  password: string;
}

/** Public sign-up of an independent therapist; the account stays pending until the superadmin approves it. */
@Injectable({ providedIn: 'root' })
export class RegistrationService {
  private readonly http = inject(HttpClient);

  register(payload: RegistrationPayload): Observable<void> {
    return this.http.post<void>(REGISTER_URL, payload);
  }
}
