import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BillingSummary, CreateBillingPayload } from './billing.models';

@Injectable({ providedIn: 'root' })
export class BillingService {
  private readonly http = inject(HttpClient);

  summary(patientId: number): Observable<BillingSummary> {
    return this.http.get<BillingSummary>(`/api/patients/${patientId}/billing`);
  }

  /** Registers a billing and answers with the updated summary. */
  register(patientId: number, payload: CreateBillingPayload): Observable<BillingSummary> {
    return this.http.post<BillingSummary>(`/api/patients/${patientId}/billings`, payload);
  }

  /** Voids a billing (only appointment sessions become pending again) and answers with the updated summary. */
  voidBilling(billingId: number): Observable<BillingSummary> {
    return this.http.post<BillingSummary>(`/api/billings/${billingId}/void`, {});
  }
}
