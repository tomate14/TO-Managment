import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SegmentMetric, SegmentMetricCount, SegmentMetricPayload } from './segment-metrics.models';

const SEGMENTS_URL = '/api/segments';
const COUNTS_URL = '/api/segment-metrics/counts';

@Injectable({ providedIn: 'root' })
export class SegmentMetricsService {
  private readonly http = inject(HttpClient);

  list(segmentId: number): Observable<SegmentMetric[]> {
    return this.http.get<SegmentMetric[]>(metricsUrl(segmentId));
  }

  create(segmentId: number, payload: SegmentMetricPayload): Observable<SegmentMetric> {
    return this.http.post<SegmentMetric>(metricsUrl(segmentId), payload);
  }

  update(segmentId: number, id: number, payload: SegmentMetricPayload): Observable<SegmentMetric> {
    return this.http.put<SegmentMetric>(`${metricsUrl(segmentId)}/${id}`, payload);
  }

  setActive(segmentId: number, id: number, active: boolean): Observable<SegmentMetric> {
    return this.http.patch<SegmentMetric>(`${metricsUrl(segmentId)}/${id}/active`, { active });
  }

  reorder(segmentId: number, ids: number[]): Observable<SegmentMetric[]> {
    return this.http.put<SegmentMetric[]>(`${metricsUrl(segmentId)}/order`, { ids });
  }

  counts(): Observable<SegmentMetricCount[]> {
    return this.http.get<SegmentMetricCount[]>(COUNTS_URL);
  }
}

function metricsUrl(segmentId: number): string {
  return `${SEGMENTS_URL}/${segmentId}/metrics`;
}
