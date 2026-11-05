import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { apiErrorMessage } from '../../../core/api/api-error';
import { CatalogItem } from '../../catalogs/catalogs.models';
import { CatalogsService } from '../../catalogs/catalogs.service';
import { Diagnosis, treatmentLabel } from '../../diagnoses/diagnoses.models';
import { DiagnosesService } from '../../diagnoses/diagnoses.service';
import { fullName, todayIso } from '../../patients/patient-format';
import { TemplateDetail } from '../../templates/templates.models';
import {
  EvaluationGroup,
  EvaluationProblems,
  createEvaluationGroup,
  evaluationPayload,
  evaluationProblems,
  hasProblems,
  metricErrorsByEvaluation,
} from '../follow-up-form';
import { CreateFollowUpPayload, Metric } from '../follow-ups.models';
import { FollowUpsService } from '../follow-ups.service';
import { SegmentEvaluationEditor } from '../segment-evaluation-editor/segment-evaluation-editor';

interface HeaderProblems {
  date?: string;
  scarTypeId?: string;
}

const SAVE_ERROR = 'No pudimos guardar el seguimiento. Intentá de nuevo.';
const LOAD_ERROR = 'No pudimos cargar el diagnóstico. Intentá de nuevo en unos minutos.';

/**
 * H6.1–H6.3 new follow-up (`/pacientes/:id/diagnosticos/:diagnosisId/seguimientos/nuevo`): date, scar type for
 * surgical treatments, and one or more segment evaluations, all saved in one request.
 */
@Component({
  selector: 'app-follow-up-form-page',
  imports: [ReactiveFormsModule, RouterLink, SegmentEvaluationEditor],
  templateUrl: './follow-up-form-page.html',
  styleUrl: './follow-up-form-page.scss',
})
export class FollowUpFormPage implements OnInit {
  private readonly followUpsService = inject(FollowUpsService);
  private readonly diagnosesService = inject(DiagnosesService);
  private readonly catalogsService = inject(CatalogsService);
  private readonly router = inject(Router);
  private readonly params = inject(ActivatedRoute).snapshot.paramMap;

  protected readonly patientId = Number(this.params.get('id'));
  protected readonly diagnosisId = Number(this.params.get('diagnosisId'));

  protected readonly evaluations = new FormArray<EvaluationGroup>([createEvaluationGroup()]);
  protected readonly form = new FormGroup({
    date: new FormControl(todayIso(), { nonNullable: true }),
    scarTypeId: new FormControl<number | null>(null),
    evaluations: this.evaluations,
  });

  protected readonly diagnosis = signal<Diagnosis | null>(null);
  protected readonly loadError = signal<string | null>(null);
  protected readonly scarTypes = signal<CatalogItem[]>([]);
  protected readonly segments = signal<CatalogItem[]>([]);
  protected readonly submitted = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly serverErrors = signal(new Map<number, Partial<Record<Metric, string>>>());
  private readonly templates = signal(new Map<EvaluationGroup, TemplateDetail>());

  private readonly value = toSignal(this.form.valueChanges.pipe(map(() => this.form.getRawValue())),
    { initialValue: this.form.getRawValue() });

  protected readonly requiresScarType = computed(() => this.diagnosis()?.treatment.requiresScarType ?? false);
  protected readonly subtitle = computed(() => {
    const diagnosis = this.diagnosis();
    return diagnosis ? `${diagnosis.description} · ${treatmentLabel(diagnosis.treatment.type)}` : '';
  });
  protected readonly patientName = computed(() => {
    const diagnosis = this.diagnosis();
    return diagnosis ? fullName(diagnosis.patient) : '';
  });
  protected readonly headerProblems = computed(() => this.validateHeader());
  protected readonly evaluationProblems = computed(() => {
    this.value();
    const templates = this.templates();
    return this.evaluations.controls.map((group) => evaluationProblems(group.getRawValue(), templates.get(group) ?? null));
  });
  /** Each evaluation offers the active segments that no other evaluation took. */
  protected readonly segmentChoices = computed(() => {
    const chosen = this.value().evaluations.map((evaluation) => evaluation.segmentId);
    return chosen.map((own) => this.segments().filter((segment) => segment.id === own || !chosen.includes(segment.id)));
  });
  protected readonly canAddSegment = computed(() => this.value().evaluations.length < this.segments().length);

  ngOnInit(): void {
    this.diagnosesService.get(this.diagnosisId).subscribe({
      next: (diagnosis) => this.diagnosis.set(diagnosis),
      error: () => this.loadError.set(LOAD_ERROR),
    });
    this.catalogsService.list('scar-types', true).subscribe({ next: (items) => this.scarTypes.set(items), error: () => undefined });
    this.catalogsService.list('body-segments', true).subscribe({ next: (items) => this.segments.set(items), error: () => undefined });
  }

  protected addSegment(): void {
    this.evaluations.push(createEvaluationGroup());
    this.serverErrors.set(new Map());
  }

  protected removeSegment(index: number): void {
    const group = this.evaluations.at(index);
    this.evaluations.removeAt(index);
    this.setTemplate(group, null);
    this.serverErrors.set(new Map());
  }

  protected setTemplate(group: EvaluationGroup, template: TemplateDetail | null): void {
    const templates = new Map(this.templates());
    if (template) {
      templates.set(group, template);
    } else {
      templates.delete(group);
    }
    this.templates.set(templates);
  }

  protected segmentsAt(index: number): CatalogItem[] {
    return this.segmentChoices().at(index) ?? [];
  }

  protected problemsAt(index: number): EvaluationProblems {
    return this.evaluationProblems()[index] ?? { answers: {} };
  }

  protected submit(): void {
    if (this.saving() || this.diagnosis() === null) {
      return;
    }
    this.submitted.set(true);
    this.error.set(null);
    this.serverErrors.set(new Map());
    const invalid = Object.keys(this.headerProblems()).length > 0 || this.evaluationProblems().some(hasProblems);
    if (invalid) {
      return;
    }
    this.saving.set(true);
    this.followUpsService.create(this.diagnosisId, this.payload()).subscribe({
      next: () => {
        this.saving.set(false);
        void this.router.navigate(['/pacientes', this.patientId, 'diagnosticos', this.diagnosisId], { replaceUrl: true });
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.serverErrors.set(metricErrorsByEvaluation(err));
        this.error.set(apiErrorMessage(err) ?? SAVE_ERROR);
      },
    });
  }

  private validateHeader(): HeaderProblems {
    const value = this.value();
    const problems: HeaderProblems = {};
    if (value.date === '') {
      problems.date = 'Ingresá la fecha del seguimiento.';
    }
    if (this.requiresScarType() && value.scarTypeId === null) {
      problems.scarTypeId = 'Elegí el tipo de cicatriz.';
    }
    return problems;
  }

  private payload(): CreateFollowUpPayload {
    const value = this.form.getRawValue();
    const templates = this.templates();
    return {
      date: value.date,
      scarTypeId: this.requiresScarType() ? Number(value.scarTypeId) : null,
      evaluations: this.evaluations.controls.map((group) =>
        evaluationPayload(group.getRawValue(), templates.get(group) ?? null)),
    };
  }
}
