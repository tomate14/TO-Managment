import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../../core/api/api-error';
import { CatalogsService } from '../../catalogs/catalogs.service';
import { SegmentMetricForm } from '../segment-metric-form/segment-metric-form';
import { SegmentMetric } from '../segment-metrics.models';
import { SegmentMetricsService } from '../segment-metrics.service';

const SEGMENTS_SLUG = 'body-segments';
const LOAD_ERROR = 'No pudimos cargar la métrica. Intentá de nuevo en unos minutos.';
const NOT_FOUND = 'La métrica no existe.';

/**
 * H11.2 mobile: new metric (`…/metricas/nueva`) and edit metric (`…/metricas/:metricId`).
 * Desktop uses the side panel of the metrics list instead.
 */
@Component({
  selector: 'app-segment-metric-form-page',
  imports: [RouterLink, SegmentMetricForm],
  templateUrl: './segment-metric-form-page.html',
  styleUrl: './segment-metric-form-page.scss',
})
export class SegmentMetricFormPage {
  private readonly metricsService = inject(SegmentMetricsService);
  private readonly catalogsService = inject(CatalogsService);
  private readonly router = inject(Router);
  private readonly params = inject(ActivatedRoute).snapshot.paramMap;

  protected readonly segmentId = Number(this.params.get('segmentId'));
  protected readonly metricId = this.params.get('metricId') === null ? null : Number(this.params.get('metricId'));
  protected readonly isEdit = this.metricId !== null;

  protected readonly segmentName = signal<string | null>(null);
  protected readonly metric = signal<SegmentMetric | null>(null);
  protected readonly error = signal<string | null>(null);

  /** Create shows the form at once; edit waits for the metric so the form starts with its values. */
  protected readonly ready = computed(() => !this.isEdit || this.metric() !== null);

  constructor() {
    this.catalogsService.list(SEGMENTS_SLUG).subscribe({
      next: (segments) => this.segmentName.set(segments.find((segment) => segment.id === this.segmentId)?.name ?? null),
    });
    if (this.isEdit) {
      this.metricsService.list(this.segmentId).subscribe({
        next: (list) => {
          const found = list.find((candidate) => candidate.id === this.metricId) ?? null;
          this.metric.set(found);
          if (found === null) {
            this.error.set(NOT_FOUND);
          }
        },
        error: (err: unknown) => this.error.set(apiErrorMessage(err) ?? LOAD_ERROR),
      });
    }
  }

  protected backToList(): void {
    void this.router.navigate(['/catalogos/segmentos', this.segmentId]);
  }
}
