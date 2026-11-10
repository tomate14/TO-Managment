import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { BillingService } from './billing.service';

describe('BillingService', () => {
  let service: BillingService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(BillingService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    try { http.verify(); } finally { TestBed.resetTestingModule(); }
  });

  it('loads the billing summary of a patient', () => {
    service.summary(7).subscribe();
    expect(http.expectOne('/api/patients/7/billing').request.method).toBe('GET');
  });

  it('registers a billing', () => {
    const payload = { billedOn: '2026-11-08', amount: 25000.5, appointmentIds: [9, 10] };
    service.register(7, payload).subscribe();
    const request = http.expectOne('/api/patients/7/billings');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
  });

  it('voids a billing', () => {
    service.voidBilling(12).subscribe();
    const request = http.expectOne('/api/billings/12/void');
    expect(request.request.method).toBe('POST');
  });
});
