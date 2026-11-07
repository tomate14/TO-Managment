import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, map, tap } from 'rxjs';
import { EstablishmentRef, LoginResponse, SESSION_EXPIRED_QUERY, SessionEndReason, TRIAL_EXPIRED_QUERY, UserSummary, isAdminInEstablishment } from './auth.models';
import { SessionStore, StoredSession } from './session-store';
import { isJwtExpired } from './jwt-expiry';

export const LOGIN_URL = '/api/auth/login';
export const REGISTER_URL = '/api/auth/register';
const ME_URL = '/api/auth/me';
const ESTABLISHMENT_URL = '/api/auth/establishment';
const PASSWORD_URL = '/api/auth/password';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly store = inject(SessionStore);
  private readonly session = signal<StoredSession | null>(this.store.load());

  readonly currentUser = computed<UserSummary | null>(() => this.session()?.user ?? null);
  readonly isSuperadmin = computed(() => this.currentUser()?.role === 'SUPERADMIN');
  readonly establishment = computed<EstablishmentRef | null>(() => this.currentUser()?.establishment ?? null);
  readonly isAdminInEstablishment = computed(() => isAdminInEstablishment(this.currentUser()));

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

  /** Superadmin only: enters an establishment (or leaves it with null); the new token replaces the session. */
  enterEstablishment(establishmentId: number | null): Observable<void> {
    return this.http.post<LoginResponse>(ESTABLISHMENT_URL, { establishmentId }).pipe(
      tap((response) => this.setSession({ token: response.token, user: response.user })),
      map(() => undefined),
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

  /** Changes the signed-in user's password; the current session stays valid. */
  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.put<void>(PASSWORD_URL, { currentPassword, newPassword });
  }

  clearSession(): void {
    this.store.clear();
    this.session.set(null);
  }

  endSession(reason: SessionEndReason): void {
    this.clearSession();
    const queryParams = reason === 'expired' ? SESSION_EXPIRED_QUERY : reason === 'trialExpired' ? TRIAL_EXPIRED_QUERY : null;
    void this.router.navigate(['/login'], queryParams ? { queryParams } : {});
  }

  /** User-initiated sign-out: drops the client token and returns to the login. */
  logout(): void {
    this.endSession('logout');
  }

  private setSession(session: StoredSession): void {
    this.store.save(session);
    this.session.set(session);
  }
}
