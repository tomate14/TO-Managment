import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, map, tap } from 'rxjs';
import { LoginResponse, SESSION_EXPIRED_QUERY, SessionEndReason, UserSummary } from './auth.models';
import { SessionStore, StoredSession } from './session-store';
import { isJwtExpired } from './jwt-expiry';

export const LOGIN_URL = '/api/auth/login';
const ME_URL = '/api/auth/me';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly store = inject(SessionStore);
  private readonly session = signal<StoredSession | null>(this.store.load());

  readonly currentUser = computed<UserSummary | null>(() => this.session()?.user ?? null);

  token(): string | null {
    return this.session()?.token ?? null;
  }

  hasValidSession(): boolean {
    const token = this.token();
    return token !== null && !isJwtExpired(token);
  }

  hasExpiredSession(): boolean {
    const token = this.token();
    return token !== null && isJwtExpired(token);
  }

  login(email: string, password: string): Observable<UserSummary> {
    return this.http.post<LoginResponse>(LOGIN_URL, { email, password }).pipe(
      tap((response) => this.setSession({ token: response.token, user: response.user })),
      map((response) => response.user),
    );
  }

  refreshCurrentUser(): void {
    this.http.get<UserSummary>(ME_URL).subscribe({
      next: (user) => {
        const current = this.session();
        if (current) {
          this.setSession({ ...current, user });
        }
      },
      error: () => undefined, // 401s are handled by the interceptor
    });
  }

  clearSession(): void {
    this.store.clear();
    this.session.set(null);
  }

  endSession(reason: SessionEndReason): void {
    this.clearSession();
    void this.router.navigate(['/login'], reason === 'expired' ? { queryParams: SESSION_EXPIRED_QUERY } : {});
  }

  private setSession(session: StoredSession): void {
    this.store.save(session);
    this.session.set(session);
  }
}
