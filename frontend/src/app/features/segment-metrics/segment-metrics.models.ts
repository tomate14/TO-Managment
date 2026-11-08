import { AnswerType } from '../templates/templates.models';

/** A ROM metric asked when a follow-up evaluates its body segment (É11). */
export interface SegmentMetric {
  id: number;
  segmentId: number;
  name: string;
  answerType: AnswerType;
  required: boolean;
  active: boolean;
  displayOrder: number;
  options: string[];
}

/** Create/edit body. `answerType` only travels on create: the backend ignores it on update. */
export interface SegmentMetricPayload {
  name: string;
  answerType?: AnswerType;
  required: boolean;
  options: string[];
}

/** Active metrics of a segment. Segments without active metrics are omitted by the backend. */
export interface SegmentMetricCount {
  segmentId: number;
  count: number;
}

export const SEGMENT_METRIC_ERROR_CODES = {
  nameTaken: 'SEGMENT_METRIC_NAME_TAKEN',
  optionsInvalid: 'SEGMENT_METRIC_OPTIONS_INVALID',
} as const;

export const SEGMENT_METRIC_NAME_TAKEN_MESSAGE = 'Ya hay una métrica con ese nombre en este segmento.';
