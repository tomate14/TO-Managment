import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateEstablishmentPayload, EstablishmentSummary } from './establishments.models';

const ESTABLISHMENTS_URL = '/api/establishments';

@Injectable({ providedIn: 'root' })
export class EstablishmentsService {
  private readonly http = inject(HttpClient);

  list(): Observable<EstablishmentSummary[]> {
    return this.http.get<EstablishmentSummary[]>(ESTABLISHMENTS_URL);
  }

  create(payload: CreateEstablishmentPayload): Observable<EstablishmentSummary> {
    return this.http.post<EstablishmentSummary>(ESTABLISHMENTS_URL, payload);
  }

  rename(id: number, name: string): Observable<EstablishmentSummary> {
    return this.http.put<EstablishmentSummary>(`${ESTABLISHMENTS_URL}/${id}`, { name });
  }

  setActive(id: number, active: boolean): Observable<EstablishmentSummary> {
    return this.http.patch<EstablishmentSummary>(`${ESTABLISHMENTS_URL}/${id}/active`, { active });
  }
}
