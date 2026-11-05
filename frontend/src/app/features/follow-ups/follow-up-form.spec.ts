import { HttpErrorResponse } from '@angular/common/http';
import { TemplateDetail, TemplateQuestion } from '../templates/templates.models';
import {
  createEvaluationGroup,
  evaluationPayload,
  evaluationProblems,
  hasProblems,
  metricErrorsByEvaluation,
  rangeHint,
  resetAnswers,
} from './follow-up-form';

function question(partial: Partial<TemplateQuestion> & Pick<TemplateQuestion, 'id' | 'answerType'>): TemplateQuestion {
  return {
    number: partial.id, depth: 0, displayOrder: partial.id, text: `Pregunta ${partial.id}`, minValue: null,
    maxValue: null, options: [], parentQuestionId: null, condition: null, active: true, locked: false,
    hasDependents: false, ...partial,
  };
}

describe('follow-up form helpers', () => {
  const template: TemplateDetail = {
    id: 5, name: 'Fractura de muñeca', segment: { id: 3, name: 'Muñeca', active: true }, active: true,
    questions: [
      question({ id: 1, answerType: 'YES_NO' }),
      question({ id: 2, answerType: 'NUMERIC', parentQuestionId: 1, minValue: 0, maxValue: 24,
        condition: { operator: 'EQUALS', expectedValue: 'true' } }),
      question({ id: 3, answerType: 'TEXT' }),
    ],
  };

  function filledGroup() {
    const group = createEvaluationGroup();
    resetAnswers(group.controls.answers, template);
    group.patchValue({ segmentId: 3, edema: '1,5', pain: 3, rom: '55', strength: '14', templateId: 5 });
    return group;
  }

  it('requires the segment, ROM and strength, and numbers where numbers go', () => {
    const problems = evaluationProblems(createEvaluationGroup().getRawValue(), null);
    expect(problems.segmentId).toBe('Elegí el segmento.');
    expect(problems.rom).toBe('Ingresá el ROM.');
    expect(problems.strength).toBe('Ingresá la fuerza.');
    expect(problems.edema).toBeUndefined();
    const group = filledGroup();
    group.patchValue({ edema: 'mucho' });
    expect(evaluationProblems(group.getRawValue(), template).edema).toBe('Ingresá un número.');
  });

  it('checks numeric answers against their range only while the question is visible', () => {
    const group = filledGroup();
    group.controls.answers.patchValue({ '1': 'true', '2': '30' });
    expect(evaluationProblems(group.getRawValue(), template).answers[2]).toBe('Entre 0 y 24.');
    group.controls.answers.patchValue({ '1': 'false' });
    expect(hasProblems(evaluationProblems(group.getRawValue(), template))).toBe(false);
  });

  it('sends typed values and drops answers of hidden questions', () => {
    const group = filledGroup();
    group.controls.answers.patchValue({ '1': 'false', '2': '8', '3': '  Mejora  ' });
    expect(evaluationPayload(group.getRawValue(), template)).toEqual({
      segmentId: 3, edema: 1.5, pain: 3, rom: 55, strength: 14, templateId: 5,
      answers: [{ questionId: 1, booleanValue: false }, { questionId: 3, textValue: 'Mejora' }],
    });
  });

  it('describes question ranges', () => {
    expect(rangeHint({ minValue: 0, maxValue: 24 })).toBe('Entre 0 y 24');
    expect(rangeHint({ minValue: 1.5, maxValue: null })).toBe('Mínimo 1,5');
    expect(rangeHint({ minValue: null, maxValue: null })).toBe('');
  });

  it('groups backend field errors by evaluation', () => {
    const error = new HttpErrorResponse({
      status: 422,
      error: { code: 'FOLLOW_UP_OUT_OF_RANGE', fieldErrors: [
        { field: 'evaluations[0].rom', message: 'ROM fuera del rango permitido.' },
        { field: 'evaluations[1].pain', message: 'Dolor fuera del rango permitido.' },
        { field: 'date', message: 'otro' },
      ] },
    });
    const errors = metricErrorsByEvaluation(error);
    expect(errors.get(0)).toEqual({ rom: 'ROM fuera del rango permitido.' });
    expect(errors.get(1)).toEqual({ pain: 'Dolor fuera del rango permitido.' });
    expect(errors.size).toBe(2);
  });
});
