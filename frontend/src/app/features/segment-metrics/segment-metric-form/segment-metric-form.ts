import { Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormArray, FormControl, NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { apiErrorCode, apiErrorMessage } from '../../../core/api/api-error';
import { answerTypeLabel } from '../../templates/question-text';
import { ANSWER_TYPES, AnswerType } from '../../templates/templates.models';
import {
  SEGMENT_METRIC_ERROR_CODES, SEGMENT_METRIC_NAME_TAKEN_MESSAGE, SegmentMetric, SegmentMetricPayload,
} from '../segment-metrics.models';
import { SegmentMetricsService } from '../segment-metrics.service';

interface Problems {
  name?: string;
  options?: string;
}

const MIN_OPTIONS = 2;
const MAX_LENGTH = 120;
const SAVE_ERROR = 'No pudimos guardar la métrica. Intentá de nuevo.';

/** H11.2 metric form shared by the mobile page and the desktop side panel. The type is chosen only on create. */
@Component({
  selector: 'app-segment-metric-form',
  imports: [ReactiveFormsModule],
  templateUrl: './segment-metric-form.html',
  styleUrl: './segment-metric-form.scss',
})
export class SegmentMetricForm implements OnInit {
  private readonly metricsService = inject(SegmentMetricsService);
  private readonly formBuilder = inject(NonNullableFormBuilder);

  readonly segmentId = input.required<number>();
  /** The metric being edited, or null to create one. Read once on init. */
  readonly metric = input<SegmentMetric | null>(null);

  readonly saved = output<SegmentMetric>();
  readonly cancelled = output<void>();

  protected readonly answerTypes = ANSWER_TYPES;
  protected readonly maxLength = MAX_LENGTH;

  protected readonly form = this.formBuilder.group({
    name: [''],
    answerType: ['NUMERIC' as AnswerType],
    required: [false],
    options: this.formBuilder.array([this.formBuilder.control(''), this.formBuilder.control('')]),
  });

  protected readonly submitted = signal(false);
  protected readonly saving = signal(false);
  protected readonly nameTaken = signal(false);
  protected readonly optionsError = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);

  private readonly value = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });

  protected readonly isEdit = computed(() => this.metric() !== null);
  protected readonly answerType = computed(() => this.value().answerType ?? 'NUMERIC');
  protected readonly required = computed(() => this.value().required ?? false);
  protected readonly problems = computed(() => this.validate());

  constructor() {
    this.form.controls.name.valueChanges.subscribe(() => this.nameTaken.set(false));
    this.form.controls.options.valueChanges.subscribe(() => this.optionsError.set(null));
  }

  ngOnInit(): void {
    const metric = this.metric();
    if (metric !== null) {
      this.options.clear();
      const labels = metric.options.length >= MIN_OPTIONS ? metric.options : ['', ''];
      labels.forEach((label) => this.options.push(this.formBuilder.control(label)));
      this.form.patchValue({ name: metric.name, answerType: metric.answerType, required: metric.required });
    }
  }

  protected get options(): FormArray<FormControl<string>> {
    return this.form.controls.options;
  }

  protected typeLabel(type: AnswerType): string {
    return answerTypeLabel(type);
  }

  protected selectType(type: AnswerType): void {
    if (!this.isEdit()) {
      this.form.controls.answerType.setValue(type);
    }
  }

  protected toggleRequired(): void {
    this.form.controls.required.setValue(!this.required());
  }

  protected addOption(): void {
    this.options.push(this.formBuilder.control(''));
  }

  protected removeOption(index: number): void {
    if (this.canRemoveOption()) {
      this.options.removeAt(index);
    }
  }

  protected canRemoveOption(): boolean {
    return this.options.length > MIN_OPTIONS;
  }

  /** Message under the name field: client validation after a save attempt, or the backend's taken name. */
  protected nameError(): string | null {
    if (this.nameTaken()) {
      return SEGMENT_METRIC_NAME_TAKEN_MESSAGE;
    }
    return this.submitted() ? (this.problems().name ?? null) : null;
  }

  protected optionsMessage(): string | null {
    return this.optionsError() ?? (this.submitted() ? (this.problems().options ?? null) : null);
  }

  protected submit(): void {
    if (this.saving()) {
      return;
    }
    this.submitted.set(true);
    this.error.set(null);
    if (Object.keys(this.problems()).length > 0) {
      return;
    }
    this.saving.set(true);
    const metric = this.metric();
    const request = metric === null
      ? this.metricsService.create(this.segmentId(), this.createPayload())
      : this.metricsService.update(this.segmentId(), metric.id, this.updatePayload());
    request.subscribe({
      next: (savedMetric) => {
        this.saving.set(false);
        this.saved.emit(savedMetric);
      },
      error: (err: unknown) => {
        this.saving.set(false);
        const code = apiErrorCode(err);
        if (code === SEGMENT_METRIC_ERROR_CODES.nameTaken) {
          this.nameTaken.set(true);
        } else if (code === SEGMENT_METRIC_ERROR_CODES.optionsInvalid) {
          this.optionsError.set(apiErrorMessage(err) ?? SAVE_ERROR);
        } else {
          this.error.set(apiErrorMessage(err) ?? SAVE_ERROR);
        }
      },
    });
  }

  private validate(): Problems {
    const problems: Problems = {};
    if ((this.value().name ?? '').trim() === '') {
      problems.name = 'Ingresá el nombre de la métrica.';
    }
    if (this.answerType() === 'OPTION') {
      const labels = this.optionLabels();
      if (labels.length < MIN_OPTIONS) {
        problems.options = 'Agregá al menos 2 opciones.';
      } else if (new Set(labels.map((label) => label.toLowerCase())).size !== labels.length) {
        problems.options = 'Las opciones no se pueden repetir.';
      }
    }
    return problems;
  }

  private optionLabels(): string[] {
    return this.options.getRawValue().map((label) => label.trim()).filter((label) => label !== '');
  }

  /** Edit body: the type cannot change, so it does not travel. */
  private updatePayload(): SegmentMetricPayload {
    const value = this.form.getRawValue();
    return {
      name: value.name.trim(),
      required: value.required,
      options: value.answerType === 'OPTION' ? this.optionLabels() : [],
    };
  }

  private createPayload(): SegmentMetricPayload {
    const { name, required, options } = this.updatePayload();
    return { name, answerType: this.form.controls.answerType.value, required, options };
  }
}
