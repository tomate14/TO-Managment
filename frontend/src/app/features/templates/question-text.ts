import { ANSWER_TYPES, AnswerType, ConditionOperator, NO_VALUE, OPERATORS, TemplateQuestion, YES_VALUE } from './templates.models';

/** Same compatibility rule as the backend: "mayor/menor que" only compare numeric parents. */
export function operatorSupports(operator: ConditionOperator, parentType: AnswerType): boolean {
  return parentType === 'NUMERIC' || operator === 'EQUALS' || operator === 'NOT_EQUALS';
}

export function answerTypeLabel(type: AnswerType): string {
  return ANSWER_TYPES.find((entry) => entry.value === type)?.label ?? type;
}

/** Chip text such as "Numérica · 0 a 24" or "Opción · 3 opciones". */
export function typeSummary(question: Pick<TemplateQuestion, 'answerType' | 'minValue' | 'maxValue' | 'options'>): string {
  const label = answerTypeLabel(question.answerType);
  if (question.answerType === 'OPTION') {
    return `${label} · ${question.options.length} opciones`;
  }
  if (question.answerType !== 'NUMERIC') {
    return label;
  }
  const { minValue, maxValue } = question;
  if (minValue !== null && maxValue !== null) {
    return `${label} · ${minValue} a ${maxValue}`;
  }
  if (minValue !== null) {
    return `${label} · desde ${minValue}`;
  }
  return maxValue !== null ? `${label} · hasta ${maxValue}` : label;
}

/** Shows an expected value the way a person reads it ("Sí"/"No" for yes/no parents). */
export function displayExpected(value: string, parentType: AnswerType): string {
  if (parentType === 'YES_NO') {
    return value === YES_VALUE ? 'Sí' : value === NO_VALUE ? 'No' : value;
  }
  return value;
}

/** Chip text for a chained question, e.g. «Si «¿Usa férula?» = Sí». */
export function conditionSummary(question: TemplateQuestion, parent: TemplateQuestion | undefined): string | null {
  if (!question.condition || !parent) {
    return null;
  }
  const symbol = OPERATORS.find((entry) => entry.value === question.condition!.operator)?.symbol ?? '';
  return `Si «${parent.text}» ${symbol} ${displayExpected(question.condition.expectedValue, parent.answerType)}`;
}

/** Preview sentence of the question form. */
export function conditionPreview(parentText: string, operator: ConditionOperator, value: string): string {
  const phrase = OPERATORS.find((entry) => entry.value === operator)?.phrase ?? '';
  return `esta pregunta aparece cuando «${parentText}» ${phrase} «${value}».`;
}

/** Parses "7,5" or "7.5"; empty text is null; anything else is NaN. */
export function parseDecimal(raw: string): number | null {
  const text = raw.trim().replace(',', '.');
  return text === '' ? null : Number(text);
}
