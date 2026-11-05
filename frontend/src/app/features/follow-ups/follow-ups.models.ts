import { CatalogItem } from '../catalogs/catalogs.models';
import { AnswerType } from '../templates/templates.models';

export interface FollowUpAnswer {
  questionId: number;
  questionText: string;
  /** 0 for top-level questions, +1 per chaining level. */
  depth: number;
  answerType: AnswerType;
  numericValue: number | null;
  booleanValue: boolean | null;
  optionValue: string | null;
  textValue: string | null;
}

export interface SegmentEvaluation {
  id: number;
  segment: CatalogItem;
  edema: number | null;
  pain: number;
  rom: number;
  strength: number;
  template: { id: number; name: string } | null;
  /** In the template tree order. */
  answers: FollowUpAnswer[];
}

export interface FollowUp {
  id: number;
  diagnosisId: number;
  /** ISO calendar date. */
  date: string;
  scarType: CatalogItem | null;
  evaluations: SegmentEvaluation[];
}

export interface AnswerPayload {
  questionId: number;
  numericValue?: number;
  booleanValue?: boolean;
  optionValue?: string;
  textValue?: string;
}

export interface SegmentEvaluationPayload {
  segmentId: number;
  edema: number | null;
  pain: number;
  rom: number;
  strength: number;
  templateId: number | null;
  answers: AnswerPayload[];
}

export interface CreateFollowUpPayload {
  date: string;
  scarTypeId: number | null;
  evaluations: SegmentEvaluationPayload[];
}

/** Clinical metrics whose out-of-range errors the backend reports per field (`evaluations[i].<metric>`). */
export type Metric = 'edema' | 'pain' | 'rom' | 'strength';

export const METRIC_LABELS: Readonly<Record<Metric, string>> = {
  edema: 'Edema',
  pain: 'Dolor',
  rom: 'ROM',
  strength: 'Fuerza',
};

export const PAIN_SCALE = { min: 0, max: 10 } as const;
