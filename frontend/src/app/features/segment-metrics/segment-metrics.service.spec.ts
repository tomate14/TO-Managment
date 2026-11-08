import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SegmentMetricsService } from './segment-metrics.service';

describe('SegmentMetricsService', () => {
  let service: SegmentMetricsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(SegmentMetricsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists the metrics of a segment', () => {
    service.list(3).subscribe();
    expect(http.expectOne('/api/segments/3/metrics').request.method).toBe('GET');
  });

  it('creates a metric', () => {
    const payload = { name: 'Sensación final', answerType: 'OPTION' as const, required: false, options: ['Firme', 'Blanda'] };
    service.create(3, payload).subscribe();
    const req = http.expectOne('/api/segments/3/metrics');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
  });

  it('updates a metric', () => {
    const payload = { name: 'Flexión', required: true, options: [] };
    service.update(3, 8, payload).subscribe();
    const req = http.expectOne('/api/segments/3/metrics/8');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(payload);
  });

  it('changes the active flag', () => {
    service.setActive(3, 8, false).subscribe();
    const req = http.expectOne('/api/segments/3/metrics/8/active');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ active: false });
  });

  it('reorders the metrics', () => {
    service.reorder(3, [9, 8, 10]).subscribe();
    const req = http.expectOne('/api/segments/3/metrics/order');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ ids: [9, 8, 10] });
  });

  it('reads the active metric counts per segment', () => {
    service.counts().subscribe();
    expect(http.expectOne('/api/segment-metrics/counts').request.method).toBe('GET');
  });
});
