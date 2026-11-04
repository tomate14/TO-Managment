import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Observable, of, switchMap } from 'rxjs';
import { apiErrorCode, apiErrorMessage } from '../../../core/api/api-error';
import { CatalogsService } from '../../catalogs/catalogs.service';
import { conditionSummary, typeSummary } from '../question-text';
import { SegmentRef, TEMPLATE_ERROR_CODES, TemplateDetail, TemplateQuestion } from '../templates.models';
import { TemplatesService } from '../templates.service';

const TEMPLATES_URL = '/plantillas';
const LOAD_ERROR = 'No pudimos cargar la plantilla.';
const SAVE_ERROR = 'No pudimos guardar la plantilla. Intentá de nuevo.';
const DUPLICATE_MESSAGE = 'Ya hay una plantilla con ese nombre en el segmento.';
const SEGMENT_INACTIVE_MESSAGE = 'Ese segmento está inactivo: elegí uno activo.';
/** Indentation per chaining level, in px (mockup: 20px), capped so deep chains still fit on a phone. */
const INDENT_PX = 20;
const MAX_INDENT_LEVELS = 3;

/** H3.1 new template (`/plantillas/nueva`) and H3.2/H3.4 template editor (`/plantillas/:id`). */
@Component({
  selector: 'app-template-edit-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './template-edit-page.html',
  styleUrl: './template-edit-page.scss',
})
export class TemplateEditPage implements OnInit {
  private readonly templatesService = inject(TemplatesService);
  private readonly catalogsService = inject(CatalogsService);
  private readonly router = inject(Router);
  private readonly idParam = inject(ActivatedRoute).snapshot.paramMap.get('id');

  protected readonly templateId = this.idParam === null ? null : Number(this.idParam);
  protected readonly isEdit = this.templateId !== null;

  protected readonly form = inject(NonNullableFormBuilder).group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    segmentId: [null as number | null, [Validators.required]],
  });
  protected readonly active = signal(true);

  protected readonly template = signal<TemplateDetail | null>(null);
  protected readonly segments = signal<SegmentRef[]>([]);
  protected readonly loading = signal(this.isEdit);
  protected readonly saving = signal(false);
  protected readonly submitted = signal(false);
  protected readonly nameError = signal<string | null>(null);
  protected readonly segmentError = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly pendingIds = signal<ReadonlySet<number>>(new Set());

  protected readonly questions = computed(() => this.template()?.questions ?? []);

  constructor() {
    this.form.controls.name.valueChanges.subscribe(() => this.nameError.set(null));
    this.form.controls.segmentId.valueChanges.subscribe(() => this.segmentError.set(null));
  }

  ngOnInit(): void {
    this.catalogsService.list('body-segments', true).subscribe({
      next: (segments) => this.mergeSegments(segments.map((segment) => ({ ...segment }))),
      error: () => this.error.set(LOAD_ERROR),
    });
    if (this.templateId !== null) {
      this.templatesService.get(this.templateId).subscribe({
        next: (template) => {
          this.applyTemplate(template);
          this.form.patchValue({ name: template.name, segmentId: template.segment.id });
          this.active.set(template.active);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
          this.error.set(LOAD_ERROR);
        },
      });
    }
  }

  protected toggleActive(): void {
    this.active.update((active) => !active);
  }

  protected showNameRequired(): boolean {
    return this.submitted() && this.form.controls.name.value.trim() === '';
  }

  protected showSegmentRequired(): boolean {
    return this.submitted() && this.form.controls.segmentId.value === null;
  }

  protected save(): void {
    if (this.saving() || this.loading()) {
      return;
    }
    this.submitted.set(true);
    this.error.set(null);
    const name = this.form.controls.name.value.trim();
    const segmentId = this.form.controls.segmentId.value;
    if (name === '' || segmentId === null) {
      return;
    }
    this.saving.set(true);
    const payload = { name, segmentId: Number(segmentId) };
    const request: Observable<TemplateDetail> = this.templateId === null
      ? this.templatesService.create(payload)
      : this.templatesService.update(this.templateId, payload).pipe(
          switchMap((saved) => saved.active === this.active()
            ? of(saved)
            : this.templatesService.setActive(saved.id, this.active())),
        );
    request.subscribe({
      next: (saved) => {
        this.saving.set(false);
        const destination = this.templateId === null ? [TEMPLATES_URL, saved.id] : [TEMPLATES_URL];
        void this.router.navigate(destination, { replaceUrl: this.templateId === null });
      },
      error: (err: unknown) => {
        this.saving.set(false);
        const code = apiErrorCode(err);
        if (code === TEMPLATE_ERROR_CODES.duplicate) {
          this.nameError.set(DUPLICATE_MESSAGE);
        } else if (code === TEMPLATE_ERROR_CODES.segmentInactive) {
          this.segmentError.set(SEGMENT_INACTIVE_MESSAGE);
        } else {
          this.error.set(apiErrorMessage(err) ?? SAVE_ERROR);
        }
      },
    });
  }

  protected toggleQuestion(question: TemplateQuestion): void {
    if (this.templateId === null || this.pendingIds().has(question.id)) {
      return;
    }
    this.error.set(null);
    this.setPending(question.id, true);
    this.templatesService.setQuestionActive(this.templateId, question.id, !question.active).subscribe({
      next: (template) => {
        this.setPending(question.id, false);
        this.applyTemplate(template);
      },
      error: (err: unknown) => {
        this.setPending(question.id, false);
        this.error.set(apiErrorMessage(err) ?? SAVE_ERROR);
      },
    });
  }

  protected isPending(question: TemplateQuestion): boolean {
    return this.pendingIds().has(question.id);
  }

  protected indent(question: TemplateQuestion): number {
    return Math.min(question.depth, MAX_INDENT_LEVELS) * INDENT_PX;
  }

  protected typeOf(question: TemplateQuestion): string {
    return typeSummary(question);
  }

  protected conditionOf(question: TemplateQuestion): string | null {
    return conditionSummary(question, this.questions().find((candidate) => candidate.id === question.parentQuestionId));
  }

  private applyTemplate(template: TemplateDetail): void {
    this.template.set(template);
    this.mergeSegments([template.segment]);
  }

  /** Keeps the template's current segment selectable even if it was deactivated later. */
  private mergeSegments(extra: SegmentRef[]): void {
    this.segments.update((current) => {
      const byId = new Map(current.map((segment) => [segment.id, segment]));
      extra.forEach((segment) => byId.set(segment.id, segment));
      return [...byId.values()].sort((a, b) => a.name.localeCompare(b.name, 'es', { sensitivity: 'base' }));
    });
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
