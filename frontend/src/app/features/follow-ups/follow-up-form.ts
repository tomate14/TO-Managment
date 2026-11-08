import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, FormRecord } from '@angular/forms';
import { SegmentMetric } from '../segment-metrics/segment-metrics.models';
import { TemplateDetail, TemplateQuestion } from '../templates/templates.models';
import { formatNumber, parseDecimal } from './follow-up-format';
import { AnswerPayload, Metric, MetricValuePayload, PAIN_SCALE, SegmentEvaluationPayload } from './follow-ups.models';
import { RawAnswers, visibleQuestionIds } from './question-visibility';

/**
 * Reactive form of one segment evaluation. Numbers are typed as text (accepting "1,5"); answers by question id and
 * ROM metric values by metric id.
 */
export interface EvaluationControls {
  segmentId: FormControl<number | null>;
  edema: FormControl<string>;
  pain: FormControl<number>;
  strength: FormControl<string>;
  templateId: FormControl<number | null>;
  answers: FormRecord<FormControl<string>>;
  metrics: FormRecord<FormControl<string>>;
}

export type EvaluationGroup = FormGroup<EvaluationControls>;
export type EvaluationValue = ReturnType<EvaluationGroup['getRawValue']>;

export function createEvaluationGroup(): EvaluationGroup {
  return new FormGroup<EvaluationControls>({
    segmentId: new FormControl<number | null>(null),
    edema: new FormControl('', { nonNullable: true }),
    pain: new FormControl<number>(PAIN_SCALE.min, { nonNullable: true }),
    strength: new FormControl('', { nonNullable: true }),
    templateId: new FormControl<number | null>(null),
    answers: new FormRecord<FormControl<string>>({}),
    metrics: new FormRecord<FormControl<string>>({}),
  });
}

/** One empty answer control per question of the template (replacing the previous template's answers). */
export function resetAnswers(answers: FormRecord<FormControl<string>>, template: TemplateDetail | null): void {
  Object.keys(answers.controls).forEach((key) => answers.removeControl(key, { emitEvent: false }));
  template?.questions.forEach((question) =>
    answers.addControl(String(question.id), new FormControl('', { nonNullable: true }), { emitEvent: false }));
  answers.updateValueAndValidity();
}

/**
 * One empty control per active ROM metric of the segment. Every control is replaced, so values typed for the previous
 * segment never travel with the new one.
 */
export function resetMetrics(metrics: FormRecord<FormControl<string>>, list: SegmentMetric[]): void {
  Object.keys(metrics.controls).forEach((key) => metrics.removeControl(key, { emitEvent: false }));
  list.filter((metric) => metric.active).forEach((metric) =>
    metrics.addControl(String(metric.id), new FormControl('', { nonNullable: true }), { emitEvent: false }));
  metrics.updateValueAndValidity();
}

export interface EvaluationProblems {
  segmentId?: string;
  edema?: string;
  strength?: string;
  /** By question id. */
  answers: Record<number, string>;
  /** By metric id. */
  metrics: Record<number, string>;
}

export function emptyProblems(): EvaluationProblems {
  return { answers: {}, metrics: {} };
}

export function hasProblems(problems: EvaluationProblems): boolean {
  const { answers, metrics, ...fields } = problems;
  return Object.keys(fields).length > 0 || Object.keys(answers).length > 0 || Object.keys(metrics).length > 0;
}

const NOT_A_NUMBER = 'Ingresá un número.';
const METRIC_REQUIRED = 'Completá esta métrica.';

/** "Entre 0 y 24", "Mínimo 0", "Máximo 24", or '' without limits. */
export function rangeHint(question: Pick<TemplateQuestion, 'minValue' | 'maxValue'>): string {
  const { minValue: min, maxValue: max } = question;
  if (min !== null && max !== null) {
    return `Entre ${formatNumber(min)} y ${formatNumber(max)}`;
  }
  if (min !== null) {
    return `Mínimo ${formatNumber(min)}`;
  }
  return max !== null ? `Máximo ${formatNumber(max)}` : '';
}

function numericAnswerProblem(question: TemplateQuestion, raw: string): string | null {
  const value = parseDecimal(raw);
  if (value === null) {
    return NOT_A_NUMBER;
  }
  const belowMin = question.minValue !== null && value < question.minValue;
  const aboveMax = question.maxValue !== null && value > question.maxValue;
  return belowMin || aboveMax ? `${rangeHint(question)}.` : null;
}

function metricProblem(metric: SegmentMetric, raw: string): string | null {
  if (raw.trim() === '') {
    return metric.required ? METRIC_REQUIRED : null;
  }
  return metric.answerType === 'NUMERIC' && parseDecimal(raw) === null ? NOT_A_NUMBER : null;
}

/**
 * Client-side checks: required fields and ROM metrics, number formats, and numeric answers within their question
 * range. Clinical ranges are checked by the backend, which returns one message per field.
 */
export function evaluationProblems(
  value: EvaluationValue,
  template: TemplateDetail | null,
  metrics: SegmentMetric[],
): EvaluationProblems {
  const problems = emptyProblems();
  if (value.segmentId === null) {
    problems.segmentId = 'Elegí el segmento.';
  }
  if (value.edema.trim() !== '' && parseDecimal(value.edema) === null) {
    problems.edema = NOT_A_NUMBER;
  }
  if (value.strength.trim() === '') {
    problems.strength = 'Ingresá la fuerza.';
  } else if (parseDecimal(value.strength) === null) {
    problems.strength = NOT_A_NUMBER;
  }
  if (template) {
    const visible = visibleQuestionIds(template.questions, value.answers);
    for (const question of template.questions) {
      const raw = value.answers[String(question.id)] ?? '';
      if (question.answerType === 'NUMERIC' && visible.has(question.id) && raw.trim() !== '') {
        const problem = numericAnswerProblem(question, raw);
        if (problem) {
          problems.answers[question.id] = problem;
        }
      }
    }
  }
  for (const metric of metrics) {
    const problem = metricProblem(metric, value.metrics[String(metric.id)] ?? '');
    if (problem) {
      problems.metrics[metric.id] = problem;
    }
  }
  return problems;
}

function answerPayload(question: TemplateQuestion, raw: string): AnswerPayload {
  switch (question.answerType) {
    case 'NUMERIC':
      return { questionId: question.id, numericValue: parseDecimal(raw) ?? undefined };
    case 'YES_NO':
      return { questionId: question.id, booleanValue: raw === 'true' };
    case 'OPTION':
      return { questionId: question.id, optionValue: raw };
    case 'TEXT':
      return { questionId: question.id, textValue: raw.trim() };
  }
}

/**
 * Only answers to visible questions are sent: answers of children hidden by a later change of their parent stay in
 * the form while it is open, but are dropped here.
 */
export function visibleAnswers(template: TemplateDetail, answers: RawAnswers): AnswerPayload[] {
  const visible = visibleQuestionIds(template.questions, answers);
  return template.questions
    .filter((question) => visible.has(question.id) && (answers[String(question.id)] ?? '').trim() !== '')
    .map((question) => answerPayload(question, answers[String(question.id)]));
}

function metricPayload(metric: SegmentMetric, raw: string): MetricValuePayload {
  switch (metric.answerType) {
    case 'NUMERIC':
      return { metricId: metric.id, numericValue: parseDecimal(raw) ?? undefined };
    case 'YES_NO':
      return { metricId: metric.id, booleanValue: raw === 'true' };
    case 'OPTION':
      return { metricId: metric.id, optionValue: raw };
    case 'TEXT':
      return { metricId: metric.id, textValue: raw.trim() };
  }
}

/** Filled ROM metrics only: the backend treats a missing optional metric as not recorded. */
export function filledMetrics(metrics: SegmentMetric[], values: RawAnswers): MetricValuePayload[] {
  return metrics
    .filter((metric) => (values[String(metric.id)] ?? '').trim() !== '')
    .map((metric) => metricPayload(metric, values[String(metric.id)]));
}

/** Call only when `evaluationProblems` found nothing. */
export function evaluationPayload(
  value: EvaluationValue,
  template: TemplateDetail | null,
  metrics: SegmentMetric[],
): SegmentEvaluationPayload {
  return {
    segmentId: Number(value.segmentId),
    edema: parseDecimal(value.edema),
    pain: value.pain,
    strength: parseDecimal(value.strength) ?? 0,
    templateId: template ? template.id : null,
    answers: template ? visibleAnswers(template, value.answers) : [],
    metrics: filledMetrics(metrics, value.metrics),
  };
}

/** Backend messages of one evaluation: fixed fields by name and ROM metrics by metric id. */
export interface EvaluationServerErrors {
  fixed: Partial<Record<Metric, string>>;
  metrics: Record<number, string>;
}

export function emptyServerErrors(): EvaluationServerErrors {
  return { fixed: {}, metrics: {} };
}

const FIELD_ERROR_PATTERN = /^evaluations\[(\d+)\]\.(edema|pain|strength)$/;
const METRIC_ERROR_PATTERN = /^evaluations\[(\d+)\]\.metrics\[(\d+)\]$/;

/** Backend per-field errors (`evaluations[0].pain`, `evaluations[0].metrics[7]`) grouped by evaluation index. */
export function metricErrorsByEvaluation(error: unknown): Map<number, EvaluationServerErrors> {
  const result = new Map<number, EvaluationServerErrors>();
  const fieldErrors: unknown = error instanceof HttpErrorResponse ? error.error?.fieldErrors : null;
  if (!Array.isArray(fieldErrors)) {
    return result;
  }
  const errorsAt = (index: number): EvaluationServerErrors => {
    const errors = result.get(index) ?? emptyServerErrors();
    result.set(index, errors);
    return errors;
  };
  for (const item of fieldErrors) {
    if (typeof item?.field !== 'string' || typeof item.message !== 'string') {
      continue;
    }
    const fixed = FIELD_ERROR_PATTERN.exec(item.field);
    const metric = METRIC_ERROR_PATTERN.exec(item.field);
    if (fixed) {
      errorsAt(Number(fixed[1])).fixed[fixed[2] as Metric] = item.message;
    } else if (metric) {
      errorsAt(Number(metric[1])).metrics[Number(metric[2])] = item.message;
    }
  }
  return result;
}
