import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { FollowUpsService } from './follow-ups.service';
import { CreateFollowUpPayload } from './follow-ups.models';

describe('FollowUpsService', () => {
  let service: FollowUpsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(FollowUpsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists the follow-ups of a diagnosis', () => {
    service.listForDiagnosis(3).subscribe();
    expect(http.expectOne('/api/diagnoses/3/follow-ups').request.method).toBe('GET');
  });

  it('creates a follow-up', () => {
    const payload: CreateFollowUpPayload = { date: '2026-11-04', scarTypeId: null, evaluations: [] };
    service.create(3, payload).subscribe();
    const request = http.expectOne('/api/diagnoses/3/follow-ups');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
  });
});
