import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateFollowUpPayload, FollowUp } from './follow-ups.models';

@Injectable({ providedIn: 'root' })
export class FollowUpsService {
  private readonly http = inject(HttpClient);

  /** Newest first, with evaluations and answers. */
  listForDiagnosis(diagnosisId: number): Observable<FollowUp[]> {
    return this.http.get<FollowUp[]>(`/api/diagnoses/${diagnosisId}/follow-ups`);
  }

  create(diagnosisId: number, payload: CreateFollowUpPayload): Observable<FollowUp> {
    return this.http.post<FollowUp>(`/api/diagnoses/${diagnosisId}/follow-ups`, payload);
  }
}
