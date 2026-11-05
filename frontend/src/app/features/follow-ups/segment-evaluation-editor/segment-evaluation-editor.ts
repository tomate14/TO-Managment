import { Component, DestroyRef, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { CatalogItem } from '../../catalogs/catalogs.models';
import { TemplateDetail, TemplateSummary } from '../../templates/templates.models';
import { TemplatesService } from '../../templates/templates.service';
import { EvaluationGroup, EvaluationProblems, resetAnswers } from '../follow-up-form';
import { Metric, PAIN_SCALE } from '../follow-ups.models';
import { TemplateQuestions } from '../template-questions/template-questions';

const TEMPLATES_ERROR = 'No pudimos cargar las plantillas del segmento.';

/**
 * H6.2 / H6.3 one segment of a follow-up: the segment, its fixed metrics and, optionally, one active template of
 * that segment with its questions.
 */
@Component({
  selector: 'app-segment-evaluation-editor',
  imports: [ReactiveFormsModule, TemplateQuestions],
  templateUrl: './segment-evaluation-editor.html',
  styleUrl: './segment-evaluation-editor.scss',
})
export class SegmentEvaluationEditor implements OnInit {
  private readonly templatesService = inject(TemplatesService);
  private readonly destroyRef = inject(DestroyRef);

  readonly group = input.required<EvaluationGroup>();
  readonly index = input.required<number>();
  /** Active segments not chosen in the other evaluations (plus this one's own). */
  readonly segments = input.required<CatalogItem[]>();
  readonly problems = input<EvaluationProblems>({ answers: {} });
  /** Out-of-range messages returned by the backend. */
  readonly serverErrors = input<Partial<Record<Metric, string>>>({});
  readonly showProblems = input(false);
  readonly canRemove = input(true);

  readonly remove = output<void>();
  /** The answered template (or null), which the page needs to validate and send the answers. */
  readonly templateChange = output<TemplateDetail | null>();

  protected readonly painScale = PAIN_SCALE;
  protected readonly templates = signal<TemplateSummary[]>([]);
  protected readonly template = signal<TemplateDetail | null>(null);
  protected readonly templatesError = signal<string | null>(null);
  protected readonly pain = signal<number>(PAIN_SCALE.min);
  private readonly segmentId = signal<number | null>(null);

  protected readonly segmentName = computed(
    () => this.segments().find((segment) => segment.id === this.segmentId())?.name ?? '');
  protected readonly prefix = computed(() => `evaluation-${this.index()}`);

  private templateRequest?: Subscription;

  ngOnInit(): void {
    const controls = this.group().controls;
    this.segmentId.set(controls.segmentId.value);
    this.pain.set(controls.pain.value);
    controls.pain.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((value) => this.pain.set(value));
    controls.segmentId.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((segmentId) => {
      this.segmentId.set(segmentId);
      controls.templateId.setValue(null);
      this.loadTemplates(segmentId);
    });
    controls.templateId.valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((templateId) => this.loadTemplate(templateId));
    if (controls.segmentId.value !== null) {
      this.loadTemplates(controls.segmentId.value);
    }
  }

  /** Server errors win over client ones: they describe the last attempt to save. */
  protected error(metric: 'edema' | 'rom' | 'strength'): string | undefined {
    return this.serverErrors()[metric] ?? (this.showProblems() ? this.problems()[metric] : undefined);
  }

  protected painError(): string | undefined {
    return this.serverErrors().pain;
  }

  private loadTemplates(segmentId: number | null): void {
    this.templates.set([]);
    this.templatesError.set(null);
    if (segmentId === null) {
      return;
    }
    this.templatesService.listForSegment(segmentId).subscribe({
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
    this.templateRequest = this.templatesService.get(templateId).subscribe({
      next: (template) => this.applyTemplate(template),
      error: () => this.templatesError.set(TEMPLATES_ERROR),
    });
  }

  private applyTemplate(template: TemplateDetail | null): void {
    resetAnswers(this.group().controls.answers, template);
    this.template.set(template);
    this.templateChange.emit(template);
  }
}
