import { TemplateQuestion } from '../templates/templates.models';
import { conditionMet, conditionReason, visibleQuestionIds } from './question-visibility';

function question(partial: Partial<TemplateQuestion> & Pick<TemplateQuestion, 'id' | 'answerType'>): TemplateQuestion {
  return {
    number: partial.id, depth: 0, displayOrder: partial.id, text: `Pregunta ${partial.id}`, minValue: null,
    maxValue: null, options: [], parentQuestionId: null, condition: null, active: true, locked: false,
    hasDependents: false, ...partial,
  };
}

describe('question visibility', () => {
  const splint = question({ id: 1, answerType: 'YES_NO' });
  const hours = question({ id: 2, answerType: 'NUMERIC', depth: 1, parentQuestionId: 1,
    condition: { operator: 'EQUALS', expectedValue: 'true' } });
  const night = question({ id: 3, answerType: 'YES_NO', depth: 2, parentQuestionId: 2,
    condition: { operator: 'GREATER_THAN', expectedValue: '12' } });
  const grip = question({ id: 4, answerType: 'OPTION', options: ['Cilíndrico', 'Esférico'] });
  const questions = [splint, hours, night, grip];

  it('shows roots and hides children of unanswered parents', () => {
    expect([...visibleQuestionIds(questions, {})]).toEqual([1, 4]);
  });

  it('shows children as soon as the parent answer satisfies the condition, chaining further', () => {
    expect([...visibleQuestionIds(questions, { '1': 'true' })]).toEqual([1, 2, 4]);
    expect([...visibleQuestionIds(questions, { '1': 'true', '2': '16' })]).toEqual([1, 2, 3, 4]);
  });

  it('answering No hides the whole branch even if children had answers', () => {
    expect([...visibleQuestionIds(questions, { '1': 'false', '2': '16' })]).toEqual([1, 4]);
  });

  it('hides inactive questions and their children', () => {
    expect([...visibleQuestionIds([{ ...splint, active: false }, hours, night, grip], { '1': 'true' })]).toEqual([4]);
  });

  it('compares like the backend', () => {
    expect(conditionMet({ operator: 'EQUALS', expectedValue: '8' }, hours, '8,0')).toBe(true);
    expect(conditionMet({ operator: 'LESS_THAN', expectedValue: '8' }, hours, '7.5')).toBe(true);
    expect(conditionMet({ operator: 'EQUALS', expectedValue: 'cilíndrico' }, grip, 'Cilíndrico')).toBe(true);
    expect(conditionMet({ operator: 'NOT_EQUALS', expectedValue: 'Esférico' }, grip, 'Cilíndrico')).toBe(true);
    expect(conditionMet({ operator: 'NOT_EQUALS', expectedValue: 'Esférico' }, grip, '')).toBe(false);
  });

  it('explains why a child is shown', () => {
    expect(conditionReason({ operator: 'EQUALS', expectedValue: 'true' }, splint)).toBe('Porque respondiste Sí');
    expect(conditionReason({ operator: 'GREATER_THAN', expectedValue: '12' }, hours)).toBe('Porque respondiste más de 12');
    expect(conditionReason({ operator: 'NOT_EQUALS', expectedValue: 'Esférico' }, grip)).toBe('Porque no respondiste Esférico');
  });
});
