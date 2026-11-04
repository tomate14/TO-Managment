import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateDiagnosisPayload, Diagnosis } from './diagnoses.models';

@Injectable({ providedIn: 'root' })
export class DiagnosesService {
  private readonly http = inject(HttpClient);

  /** Newest first. */
  listForPatient(patientId: number): Observable<Diagnosis[]> {
    return this.http.get<Diagnosis[]>(`/api/patients/${patientId}/diagnoses`);
  }

  create(patientId: number, payload: CreateDiagnosisPayload): Observable<Diagnosis> {
    return this.http.post<Diagnosis>(`/api/patients/${patientId}/diagnoses`, payload);
  }

  get(id: number): Observable<Diagnosis> {
    return this.http.get<Diagnosis>(`/api/diagnoses/${id}`);
  }
}
