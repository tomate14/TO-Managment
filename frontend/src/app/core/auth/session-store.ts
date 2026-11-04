import { Injectable } from '@angular/core';
import { UserSummary } from './auth.models';

export interface StoredSession {
  token: string;
  user: UserSummary;
}

const STORAGE_KEY = 'to-management.session';

/** Persists the session (token + user, never the password). Storage can be unavailable: failures are ignored. */
@Injectable({ providedIn: 'root' })
export class SessionStore {
  load(): StoredSession | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as StoredSession) : null;
    } catch {
      return null;
    }
  }

  save(session: StoredSession): void {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    } catch {
      /* private mode / blocked storage: session lives only in memory */
    }
  }

  clear(): void {
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      /* ignore */
    }
  }
}
