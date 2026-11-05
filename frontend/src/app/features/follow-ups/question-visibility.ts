import { Condition, NO_VALUE, TemplateQuestion, YES_VALUE } from '../templates/templates.models';
import { formatNumber, parseDecimal } from './follow-up-format';

/**
 * Raw answers of the template form, by question id: numbers as typed, yes/no as "true"/"false", option labels and
 * free text. An empty string means unanswered.
 */
export type RawAnswers = Readonly<Record<string, string>>;

/** Mirrors the backend `AnswerValue.matches`: numbers by value, yes/no by "true"/"false", the rest ignoring case. */
function matches(question: TemplateQuestion, raw: string, expected: string): boolean {
  switch (question.answerType) {
    case 'NUMERIC': {
      const answer = parseDecimal(raw);
      const target = parseDecimal(expected);
      return answer !== null && target !== null && answer === target;
    }
    case 'YES_NO':
      return raw === expected.trim().toLowerCase();
    case 'OPTION':
    case 'TEXT':
      return raw.trim().toLowerCase() === expected.trim().toLowerCase();
  }
}

/** Mirrors the backend activation conditions: an unanswered parent never activates its children. */
export function conditionMet(condition: Condition, parent: TemplateQuestion, raw: string | undefined): boolean {
  const answer = raw ?? '';
  if (answer.trim() === '') {
    return false;
  }
  switch (condition.operator) {
    case 'EQUALS':
      return matches(parent, answer, condition.expectedValue);
    case 'NOT_EQUALS':
      return !matches(parent, answer, condition.expectedValue);
    case 'GREATER_THAN':
    case 'LESS_THAN': {
      const value = parseDecimal(answer);
      const limit = parseDecimal(condition.expectedValue);
      if (value === null || limit === null) {
        return false;
      }
      return condition.operator === 'GREATER_THAN' ? value > limit : value < limit;
    }
  }
}

/**
 * Same rule as the backend `VisibleQuestionResolver`: a question is shown when it is active and either has no
 * parent, or its parent is shown and its answer satisfies the condition. Children of inactive questions stay hidden.
 *
 * @param questions every question of the template, in tree order (each parent before its children)
 */
export function visibleQuestionIds(questions: readonly TemplateQuestion[], answers: RawAnswers): Set<number> {
  const byId = new Map(questions.map((question) => [question.id, question]));
  const visible = new Set<number>();
  for (const question of questions) {
    if (!question.active) {
      continue;
    }
    if (question.parentQuestionId === null) {
      visible.add(question.id);
      continue;
    }
    const parent = byId.get(question.parentQuestionId);
    if (parent && visible.has(parent.id) && question.condition
        && conditionMet(question.condition, parent, answers[String(parent.id)])) {
      visible.add(question.id);
    }
  }
  return visible;
}

function displayValue(parent: TemplateQuestion, expected: string): string {
  if (parent.answerType === 'YES_NO') {
    return expected === YES_VALUE ? 'Sí' : expected === NO_VALUE ? 'No' : expected;
  }
  if (parent.answerType === 'NUMERIC') {
    const value = parseDecimal(expected);
    return value === null ? expected : formatNumber(value);
  }
  return expected;
}

/** Why a child question is shown: "Porque respondiste Sí", "Porque respondiste más de 12"… */
export function conditionReason(condition: Condition, parent: TemplateQuestion): string {
  const value = displayValue(parent, condition.expectedValue);
  switch (condition.operator) {
    case 'EQUALS':
      return `Porque respondiste ${value}`;
    case 'NOT_EQUALS':
      return `Porque no respondiste ${value}`;
    case 'GREATER_THAN':
      return `Porque respondiste más de ${value}`;
    case 'LESS_THAN':
      return `Porque respondiste menos de ${value}`;
  }
}
