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

/** A recorded value of one ROM metric of the segment (É11). */
export interface FollowUpMetricValue {
  metricId: number;
  name: string;
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
  /** Fixed ROM of follow-ups recorded before É11; null on newer ones, which record `metrics` instead. */
  rom: number | null;
  strength: number;
  template: { id: number; name: string } | null;
  /** In the template tree order. */
  answers: FollowUpAnswer[];
  /** Recorded ROM metric values of the segment, in the metrics order. */
  metrics: FollowUpMetricValue[];
}

export interface FollowUp {
  id: number;
  diagnosisId: number;
  /** ISO calendar date. */
  date: string;
  scarType: CatalogItem | null;
  evaluations: SegmentEvaluation[];
  /** Whether the session was billed (E12). */
  billed: boolean;
}

export interface AnswerPayload {
  questionId: number;
  numericValue?: number;
  booleanValue?: boolean;
  optionValue?: string;
  textValue?: string;
}

export interface MetricValuePayload {
  metricId: number;
  numericValue?: number;
  booleanValue?: boolean;
  optionValue?: string;
  textValue?: string;
}

export interface SegmentEvaluationPayload {
  segmentId: number;
  edema: number | null;
  pain: number;
  strength: number;
  templateId: number | null;
  answers: AnswerPayload[];
  /** Only the filled metrics. */
  metrics: MetricValuePayload[];
}

export interface CreateFollowUpPayload {
  date: string;
  scarTypeId: number | null;
  evaluations: SegmentEvaluationPayload[];
}

/** Clinical metrics whose out-of-range errors the backend reports per field (`evaluations[i].<metric>`). */
export type Metric = 'edema' | 'pain' | 'strength';

export const METRIC_LABELS: Readonly<Record<Metric, string>> = {
  edema: 'Edema',
  pain: 'Dolor',
  strength: 'Fuerza',
};

export const PAIN_SCALE = { min: 0, max: 10 } as const;
