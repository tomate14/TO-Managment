import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { BillingService } from './billing.service';
import { CreateBillingPayload } from './billing.models';

describe('BillingService', () => {
  let service: BillingService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(BillingService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the billing summary of a diagnosis', () => {
    service.summary(3).subscribe();
    expect(http.expectOne('/api/diagnoses/3/billing').request.method).toBe('GET');
  });

  it('registers a billing', () => {
    const payload: CreateBillingPayload = { billedOn: '2026-11-08', amount: 25000.5, followUpIds: [9, 10] };
    service.register(3, payload).subscribe();
    const request = http.expectOne('/api/diagnoses/3/billings');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
  });

  it('voids a billing', () => {
    service.voidBilling(12).subscribe();
    const request = http.expectOne('/api/billings/12/void');
    expect(request.request.method).toBe('POST');
  });
});
