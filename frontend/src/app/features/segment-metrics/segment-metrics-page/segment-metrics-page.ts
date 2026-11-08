import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../../core/api/api-error';
import { injectIsDesktop } from '../../../layout/desktop-media';
import { CatalogsService } from '../../catalogs/catalogs.service';
import { answerTypeLabel } from '../../templates/question-text';
import { SegmentMetricForm } from '../segment-metric-form/segment-metric-form';
import { SegmentMetric } from '../segment-metrics.models';
import { SegmentMetricsService } from '../segment-metrics.service';

const SEGMENTS_SLUG = 'body-segments';
const LOAD_ERROR = 'No pudimos cargar las métricas. Intentá de nuevo en unos minutos.';
const SAVE_ERROR = 'No pudimos guardar el cambio. Intentá de nuevo.';

/** Which form the desktop side panel shows: a new metric, or the metric being edited. */
type PanelState = { mode: 'new' } | { mode: 'edit'; metric: SegmentMetric };

/** H11.1 ROM metrics of a body segment (`/catalogos/segmentos/:segmentId`): order, activation and the desktop form panel. */
@Component({
  selector: 'app-segment-metrics-page',
  imports: [RouterLink, SegmentMetricForm],
  templateUrl: './segment-metrics-page.html',
  styleUrl: './segment-metrics-page.scss',
})
export class SegmentMetricsPage {
  private readonly metricsService = inject(SegmentMetricsService);
  private readonly catalogsService = inject(CatalogsService);
  private readonly router = inject(Router);

  protected readonly segmentId = Number(inject(ActivatedRoute).snapshot.paramMap.get('segmentId'));
  protected readonly isDesktop = injectIsDesktop();

  protected readonly segmentName = signal<string | null>(null);
  protected readonly metrics = signal<SegmentMetric[] | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly reordering = signal(false);
  protected readonly pendingIds = signal<ReadonlySet<number>>(new Set());
  protected readonly panel = signal<PanelState | null>(null);

  protected readonly panelOpen = computed(() => this.panel() !== null);
  protected readonly panelMetric = computed(() => {
    const state = this.panel();
    return state?.mode === 'edit' ? state.metric : null;
  });

  constructor() {
    this.catalogsService.list(SEGMENTS_SLUG).subscribe({
      next: (segments) => this.segmentName.set(segments.find((segment) => segment.id === this.segmentId)?.name ?? null),
    });
    this.metricsService.list(this.segmentId).subscribe({
      next: (list) => this.metrics.set(list),
      error: (err: unknown) => this.error.set(apiErrorMessage(err) ?? LOAD_ERROR),
    });
  }

  protected typeChip(metric: SegmentMetric): string {
    const label = answerTypeLabel(metric.answerType);
    return metric.answerType === 'OPTION' ? `${label} · ${metric.options.length}` : label;
  }

  protected openNew(): void {
    if (this.isDesktop()) {
      this.panel.set({ mode: 'new' });
    } else {
      void this.router.navigate(['/catalogos/segmentos', this.segmentId, 'metricas', 'nueva']);
    }
  }

  protected openEdit(metric: SegmentMetric): void {
    if (this.isDesktop()) {
      this.panel.set({ mode: 'edit', metric });
    } else {
      void this.router.navigate(['/catalogos/segmentos', this.segmentId, 'metricas', metric.id]);
    }
  }

  protected closePanel(): void {
    this.panel.set(null);
  }

  protected onSaved(saved: SegmentMetric): void {
    this.panel.set(null);
    this.metrics.update((list) => {
      const current = list ?? [];
      return current.some((metric) => metric.id === saved.id)
        ? current.map((metric) => (metric.id === saved.id ? saved : metric))
        : [...current, saved];
    });
  }

  /** Moves a metric one place up (-1) or down (+1), optimistically; restores the old order if the backend refuses. */
  protected move(index: number, delta: -1 | 1): void {
    const previous = this.metrics();
    const target = index + delta;
    if (previous === null || this.reordering() || target < 0 || target >= previous.length) {
      return;
    }
    const next = [...previous];
    [next[index], next[target]] = [next[target], next[index]];
    this.metrics.set(next);
    this.error.set(null);
    this.reordering.set(true);
    this.metricsService.reorder(this.segmentId, next.map((metric) => metric.id)).subscribe({
      next: (list) => {
        this.reordering.set(false);
        this.metrics.set(list);
      },
      error: (err: unknown) => {
        this.reordering.set(false);
        this.metrics.set(previous);
        this.error.set(apiErrorMessage(err) ?? SAVE_ERROR);
      },
    });
  }

  protected toggle(metric: SegmentMetric): void {
    if (this.isPending(metric)) {
      return;
    }
    this.error.set(null);
    this.setPending(metric.id, true);
    this.metricsService.setActive(this.segmentId, metric.id, !metric.active).subscribe({
      next: (updated) => {
        this.setPending(metric.id, false);
        this.metrics.update((list) => (list ?? []).map((item) => (item.id === updated.id ? updated : item)));
      },
      error: (err: unknown) => {
        this.setPending(metric.id, false);
        this.error.set(apiErrorMessage(err) ?? SAVE_ERROR);
      },
    });
  }

  protected isPending(metric: SegmentMetric): boolean {
    return this.pendingIds().has(metric.id);
  }

  private setPending(id: number, pending: boolean): void {
    this.pendingIds.update((ids) => {
      const next = new Set(ids);
      if (pending) {
        next.add(id);
      } else {
        next.delete(id);
      }
      return next;
    });
  }
}
