import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, FormRecord } from '@angular/forms';
import { TemplateDetail, TemplateQuestion } from '../templates/templates.models';
import { formatNumber, parseDecimal } from './follow-up-format';
import { AnswerPayload, Metric, PAIN_SCALE, SegmentEvaluationPayload } from './follow-ups.models';
import { RawAnswers, visibleQuestionIds } from './question-visibility';

/** Reactive form of one segment evaluation. Numbers are typed as text (accepting "1,5"); answers by question id. */
export interface EvaluationControls {
  segmentId: FormControl<number | null>;
  edema: FormControl<string>;
  pain: FormControl<number>;
  rom: FormControl<string>;
  strength: FormControl<string>;
  templateId: FormControl<number | null>;
  answers: FormRecord<FormControl<string>>;
}

export type EvaluationGroup = FormGroup<EvaluationControls>;
export type EvaluationValue = ReturnType<EvaluationGroup['getRawValue']>;

export function createEvaluationGroup(): EvaluationGroup {
  return new FormGroup<EvaluationControls>({
    segmentId: new FormControl<number | null>(null),
    edema: new FormControl('', { nonNullable: true }),
    pain: new FormControl<number>(PAIN_SCALE.min, { nonNullable: true }),
    rom: new FormControl('', { nonNullable: true }),
    strength: new FormControl('', { nonNullable: true }),
    templateId: new FormControl<number | null>(null),
    answers: new FormRecord<FormControl<string>>({}),
  });
}

/** One empty answer control per question of the template (replacing the previous template's answers). */
export function resetAnswers(answers: FormRecord<FormControl<string>>, template: TemplateDetail | null): void {
  Object.keys(answers.controls).forEach((key) => answers.removeControl(key, { emitEvent: false }));
  template?.questions.forEach((question) =>
    answers.addControl(String(question.id), new FormControl('', { nonNullable: true }), { emitEvent: false }));
  answers.updateValueAndValidity();
}

export interface EvaluationProblems {
  segmentId?: string;
  edema?: string;
  rom?: string;
  strength?: string;
  /** By question id. */
  answers: Record<number, string>;
}

export function hasProblems(problems: EvaluationProblems): boolean {
  const { answers, ...fields } = problems;
  return Object.keys(fields).length > 0 || Object.keys(answers).length > 0;
}

const NOT_A_NUMBER = 'Ingresá un número.';

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

/**
 * Client-side checks: required metrics and number formats, and numeric answers within their question range.
 * Clinical ranges are checked by the backend, which returns one message per field.
 */
export function evaluationProblems(value: EvaluationValue, template: TemplateDetail | null): EvaluationProblems {
  const problems: EvaluationProblems = { answers: {} };
  if (value.segmentId === null) {
    problems.segmentId = 'Elegí el segmento.';
  }
  if (value.edema.trim() !== '' && parseDecimal(value.edema) === null) {
    problems.edema = NOT_A_NUMBER;
  }
  if (value.rom.trim() === '') {
    problems.rom = 'Ingresá el ROM.';
  } else if (parseDecimal(value.rom) === null) {
    problems.rom = NOT_A_NUMBER;
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

/** Call only when `evaluationProblems` found nothing. */
export function evaluationPayload(value: EvaluationValue, template: TemplateDetail | null): SegmentEvaluationPayload {
  return {
    segmentId: Number(value.segmentId),
    edema: parseDecimal(value.edema),
    pain: value.pain,
    rom: parseDecimal(value.rom) ?? 0,
    strength: parseDecimal(value.strength) ?? 0,
    templateId: template ? template.id : null,
    answers: template ? visibleAnswers(template, value.answers) : [],
  };
}

const FIELD_ERROR_PATTERN = /^evaluations\[(\d+)\]\.(edema|pain|rom|strength)$/;

/** Backend per-field errors (`evaluations[0].rom`) grouped by evaluation index. */
export function metricErrorsByEvaluation(error: unknown): Map<number, Partial<Record<Metric, string>>> {
  const result = new Map<number, Partial<Record<Metric, string>>>();
  const fieldErrors: unknown = error instanceof HttpErrorResponse ? error.error?.fieldErrors : null;
  if (!Array.isArray(fieldErrors)) {
    return result;
  }
  for (const item of fieldErrors) {
    const match = typeof item?.field === 'string' ? FIELD_ERROR_PATTERN.exec(item.field) : null;
    if (match && typeof item.message === 'string') {
      const index = Number(match[1]);
      result.set(index, { ...result.get(index), [match[2] as Metric]: item.message });
    }
  }
  return result;
}
