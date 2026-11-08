import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BillingSummary, CreateBillingPayload } from './billing.models';

@Injectable({ providedIn: 'root' })
export class BillingService {
  private readonly http = inject(HttpClient);

  summary(diagnosisId: number): Observable<BillingSummary> {
    return this.http.get<BillingSummary>(`/api/diagnoses/${diagnosisId}/billing`);
  }

  /** Registers a billing and answers with the updated summary. */
  register(diagnosisId: number, payload: CreateBillingPayload): Observable<BillingSummary> {
    return this.http.post<BillingSummary>(`/api/diagnoses/${diagnosisId}/billings`, payload);
  }

  /** Voids a billing (its sessions become pending again) and answers with the updated summary. */
  voidBilling(billingId: number): Observable<BillingSummary> {
    return this.http.post<BillingSummary>(`/api/billings/${billingId}/void`, {});
  }
}
