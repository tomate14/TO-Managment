import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateUserPayload, ManagedUser, UpdateUserPayload } from './users.models';

const USERS_URL = '/api/users';

@Injectable({ providedIn: 'root' })
export class UsersService {
  private readonly http = inject(HttpClient);

  list(): Observable<ManagedUser[]> {
    return this.http.get<ManagedUser[]>(USERS_URL);
  }

  get(id: number): Observable<ManagedUser> {
    return this.http.get<ManagedUser>(`${USERS_URL}/${id}`);
  }

  create(payload: CreateUserPayload): Observable<ManagedUser> {
    return this.http.post<ManagedUser>(USERS_URL, payload);
  }

  update(id: number, payload: UpdateUserPayload): Observable<ManagedUser> {
    return this.http.put<ManagedUser>(`${USERS_URL}/${id}`, payload);
  }

  setActive(id: number, active: boolean): Observable<ManagedUser> {
    return this.http.patch<ManagedUser>(`${USERS_URL}/${id}/active`, { active });
  }
}
