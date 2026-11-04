export type AnswerType = 'NUMERIC' | 'YES_NO' | 'OPTION' | 'TEXT';
export type ConditionOperator = 'EQUALS' | 'NOT_EQUALS' | 'GREATER_THAN' | 'LESS_THAN';

export interface SegmentRef {
  id: number;
  name: string;
  active: boolean;
}

export interface TemplateSummary {
  id: number;
  name: string;
  segment: SegmentRef;
  active: boolean;
  questionCount: number;
  chainedCount: number;
}

export interface Condition {
  operator: ConditionOperator;
  /** Canonical text: "true"/"false" for yes/no parents. */
  expectedValue: string;
}

export interface TemplateQuestion {
  id: number;
  number: number;
  depth: number;
  displayOrder: number;
  text: string;
  answerType: AnswerType;
  minValue: number | null;
  maxValue: number | null;
  options: string[];
  parentQuestionId: number | null;
  condition: Condition | null;
  active: boolean;
  locked: boolean;
  hasDependents: boolean;
}

export interface TemplateDetail {
  id: number;
  name: string;
  segment: SegmentRef;
  active: boolean;
  questions: TemplateQuestion[];
}

export interface TemplatePayload {
  name: string;
  segmentId: number;
}

export interface QuestionPayload {
  text: string;
  answerType: AnswerType;
  minValue: number | null;
  maxValue: number | null;
  options: string[];
  parentQuestionId: number | null;
  condition: Condition | null;
}

export const ANSWER_TYPES: readonly { value: AnswerType; label: string }[] = [
  { value: 'NUMERIC', label: 'Numérica' },
  { value: 'YES_NO', label: 'Sí / No' },
  { value: 'OPTION', label: 'Opción' },
  { value: 'TEXT', label: 'Texto' },
];

export const OPERATORS: readonly { value: ConditionOperator; label: string; phrase: string; symbol: string }[] = [
  { value: 'EQUALS', label: 'Igual a', phrase: 'es igual a', symbol: '=' },
  { value: 'NOT_EQUALS', label: 'Distinto de', phrase: 'es distinta de', symbol: '≠' },
  { value: 'GREATER_THAN', label: 'Mayor que', phrase: 'es mayor que', symbol: '>' },
  { value: 'LESS_THAN', label: 'Menor que', phrase: 'es menor que', symbol: '<' },
];

export const YES_VALUE = 'true';
export const NO_VALUE = 'false';

export const TEMPLATE_ERROR_CODES = {
  duplicate: 'TEMPLATE_DUPLICATE',
  segmentInactive: 'SEGMENT_INACTIVE',
  operatorIncompatible: 'CONDITION_OPERATOR_INCOMPATIBLE',
} as const;
