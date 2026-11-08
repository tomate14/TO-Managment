import { Component, DestroyRef, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { CatalogItem } from '../../catalogs/catalogs.models';
import { SegmentMetric } from '../../segment-metrics/segment-metrics.models';
import { SegmentMetricsService } from '../../segment-metrics/segment-metrics.service';
import { TemplateDetail, TemplateSummary } from '../../templates/templates.models';
import { TemplatesService } from '../../templates/templates.service';
import {
  EvaluationGroup,
  EvaluationProblems,
  EvaluationServerErrors,
  emptyProblems,
  emptyServerErrors,
  resetAnswers,
  resetMetrics,
} from '../follow-up-form';
import { PAIN_SCALE } from '../follow-ups.models';
import { SegmentMetricsFields } from '../segment-metrics-fields/segment-metrics-fields';
import { TemplateQuestions } from '../template-questions/template-questions';

const TEMPLATES_ERROR = 'No pudimos cargar las plantillas del segmento.';
const METRICS_ERROR = 'No pudimos cargar las métricas ROM del segmento.';

/**
 * H6.2 / H6.3 / H11.3 one segment of a follow-up: the segment, its fixed metrics, its ROM metrics and, optionally,
 * one active template of that segment with its questions.
 */
@Component({
  selector: 'app-segment-evaluation-editor',
  imports: [ReactiveFormsModule, TemplateQuestions, SegmentMetricsFields],
  templateUrl: './segment-evaluation-editor.html',
  styleUrl: './segment-evaluation-editor.scss',
})
export class SegmentEvaluationEditor implements OnInit {
  private readonly templatesService = inject(TemplatesService);
  private readonly segmentMetricsService = inject(SegmentMetricsService);
  private readonly destroyRef = inject(DestroyRef);

  readonly group = input.required<EvaluationGroup>();
  readonly index = input.required<number>();
  /** Active segments not chosen in the other evaluations (plus this one's own). */
  readonly segments = input.required<CatalogItem[]>();
  readonly problems = input<EvaluationProblems>(emptyProblems());
  /** Messages returned by the backend for the last attempt to save. */
  readonly serverErrors = input<EvaluationServerErrors>(emptyServerErrors());
  readonly showProblems = input(false);
  readonly canRemove = input(true);

  readonly remove = output<void>();
  /** The answered template (or null), which the page needs to validate and send the answers. */
  readonly templateChange = output<TemplateDetail | null>();
  /** Active ROM metrics of the chosen segment, which the page needs to validate and send their values. */
  readonly metricsChange = output<SegmentMetric[]>();

  protected readonly painScale = PAIN_SCALE;
  protected readonly canDefineMetrics = inject(AuthService).isAdminInEstablishment;
  protected readonly templates = signal<TemplateSummary[]>([]);
  protected readonly template = signal<TemplateDetail | null>(null);
  protected readonly templatesError = signal<string | null>(null);
  /** Null until the metrics of the chosen segment arrive. */
  protected readonly metrics = signal<SegmentMetric[] | null>(null);
  protected readonly metricsError = signal<string | null>(null);
  protected readonly pain = signal<number>(PAIN_SCALE.min);
  protected readonly segmentId = signal<number | null>(null);

  protected readonly segmentName = computed(
    () => this.segments().find((segment) => segment.id === this.segmentId())?.name ?? '');
  protected readonly prefix = computed(() => `evaluation-${this.index()}`);
  /** Server messages win over client ones: they describe the last attempt to save. */
  protected readonly metricProblems = computed(() => ({ ...this.problems().metrics, ...this.serverErrors().metrics }));

  private templatesRequest?: Subscription;
  private templateRequest?: Subscription;
  private metricsRequest?: Subscription;

  ngOnInit(): void {
    const controls = this.group().controls;
    this.segmentId.set(controls.segmentId.value);
    this.pain.set(controls.pain.value);
    controls.pain.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((value) => this.pain.set(value));
    controls.segmentId.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((segmentId) => {
      this.segmentId.set(segmentId);
      controls.templateId.setValue(null);
      this.loadTemplates(segmentId);
      this.loadMetrics(segmentId);
    });
    controls.templateId.valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((templateId) => this.loadTemplate(templateId));
    if (controls.segmentId.value !== null) {
      this.loadTemplates(controls.segmentId.value);
      this.loadMetrics(controls.segmentId.value);
    }
  }

  /** Server errors win over client ones: they describe the last attempt to save. */
  protected error(metric: 'edema' | 'strength'): string | undefined {
    return this.serverErrors().fixed[metric] ?? (this.showProblems() ? this.problems()[metric] : undefined);
  }

  protected painError(): string | undefined {
    return this.serverErrors().fixed.pain;
  }

  private loadTemplates(segmentId: number | null): void {
    this.templatesRequest?.unsubscribe();
    this.templates.set([]);
    this.templatesError.set(null);
    if (segmentId === null) {
      return;
    }
    this.templatesRequest = this.templatesService.listForSegment(segmentId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (templates) => this.templates.set(templates.filter((template) => template.active)),
      error: () => this.templatesError.set(TEMPLATES_ERROR),
    });
  }

  private loadTemplate(templateId: number | null): void {
    this.templateRequest?.unsubscribe();
    this.applyTemplate(null);
    if (templateId === null) {
      return;
    }
    this.templateRequest = this.templatesService.get(templateId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (template) => this.applyTemplate(template),
      error: () => this.templatesError.set(TEMPLATES_ERROR),
    });
  }

  private applyTemplate(template: TemplateDetail | null): void {
    resetAnswers(this.group().controls.answers, template);
    this.template.set(template);
    this.templateChange.emit(template);
  }

  /** The values typed for the previous segment are dropped right away, before the new metrics arrive. */
  private loadMetrics(segmentId: number | null): void {
    this.metricsRequest?.unsubscribe();
    this.metricsError.set(null);
    this.applyMetrics(null);
    if (segmentId === null) {
      return;
    }
    this.metricsRequest = this.segmentMetricsService.list(segmentId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (metrics) => this.applyMetrics(
        metrics.filter((metric) => metric.active).sort((a, b) => a.displayOrder - b.displayOrder)),
      error: () => this.metricsError.set(METRICS_ERROR),
    });
  }

  private applyMetrics(metrics: SegmentMetric[] | null): void {
    resetMetrics(this.group().controls.metrics, metrics ?? []);
    this.metrics.set(metrics);
    this.metricsChange.emit(metrics ?? []);
  }
}
