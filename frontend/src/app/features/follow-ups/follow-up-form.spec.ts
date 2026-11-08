import { HttpErrorResponse } from '@angular/common/http';
import { SegmentMetric } from '../segment-metrics/segment-metrics.models';
import { TemplateDetail, TemplateQuestion } from '../templates/templates.models';
import {
  createEvaluationGroup,
  evaluationPayload,
  evaluationProblems,
  hasProblems,
  metricErrorsByEvaluation,
  rangeHint,
  resetAnswers,
  resetMetrics,
} from './follow-up-form';

function question(partial: Partial<TemplateQuestion> & Pick<TemplateQuestion, 'id' | 'answerType'>): TemplateQuestion {
  return {
    number: partial.id, depth: 0, displayOrder: partial.id, text: `Pregunta ${partial.id}`, minValue: null,
    maxValue: null, options: [], parentQuestionId: null, condition: null, active: true, locked: false,
    hasDependents: false, ...partial,
  };
}

function metric(partial: Partial<SegmentMetric> & Pick<SegmentMetric, 'id' | 'answerType'>): SegmentMetric {
  return {
    segmentId: 3, name: `Métrica ${partial.id}`, required: false, active: true, displayOrder: partial.id, options: [],
    ...partial,
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

  const metrics: SegmentMetric[] = [
    metric({ id: 7, name: 'Flexión', answerType: 'NUMERIC', required: true }),
    metric({ id: 8, name: 'Pronosupinación completa', answerType: 'YES_NO' }),
    metric({ id: 9, name: 'Sensación final', answerType: 'OPTION', options: ['Firme', 'Blanda'] }),
    metric({ id: 10, name: 'Observación', answerType: 'TEXT' }),
  ];

  function filledGroup() {
    const group = createEvaluationGroup();
    resetAnswers(group.controls.answers, template);
    resetMetrics(group.controls.metrics, metrics);
    group.patchValue({ segmentId: 3, edema: '1,5', pain: 3, strength: '14', templateId: 5 });
    group.controls.metrics.patchValue({ '7': '130' });
    return group;
  }

  it('requires the segment and strength, and numbers where numbers go', () => {
    const problems = evaluationProblems(createEvaluationGroup().getRawValue(), null, []);
    expect(problems.segmentId).toBe('Elegí el segmento.');
    expect(problems.strength).toBe('Ingresá la fuerza.');
    expect(problems.edema).toBeUndefined();
    expect('rom' in problems).toBe(false);
    const group = filledGroup();
    group.patchValue({ edema: 'mucho' });
    expect(evaluationProblems(group.getRawValue(), template, metrics).edema).toBe('Ingresá un número.');
  });

  it('leaves one control per active metric, dropping the previous values', () => {
    const group = filledGroup();
    resetMetrics(group.controls.metrics, [metric({ id: 20, answerType: 'NUMERIC' }),
      metric({ id: 21, answerType: 'NUMERIC', active: false })]);
    expect(group.controls.metrics.getRawValue()).toEqual({ '20': '' });
  });

  it('asks for the required metrics and numbers in numeric metrics', () => {
    const group = filledGroup();
    group.controls.metrics.patchValue({ '7': '' });
    expect(evaluationProblems(group.getRawValue(), null, metrics).metrics).toEqual({ 7: 'Completá esta métrica.' });
    group.controls.metrics.patchValue({ '7': 'mucho' });
    expect(evaluationProblems(group.getRawValue(), null, metrics).metrics).toEqual({ 7: 'Ingresá un número.' });
    group.controls.metrics.patchValue({ '7': '130' });
    expect(hasProblems(evaluationProblems(group.getRawValue(), null, metrics))).toBe(false);
  });

  it('checks numeric answers against their range only while the question is visible', () => {
    const group = filledGroup();
    group.controls.answers.patchValue({ '1': 'true', '2': '30' });
    expect(evaluationProblems(group.getRawValue(), template, metrics).answers[2]).toBe('Entre 0 y 24.');
    group.controls.answers.patchValue({ '1': 'false' });
    expect(hasProblems(evaluationProblems(group.getRawValue(), template, metrics))).toBe(false);
  });

  it('sends typed values and drops answers of hidden questions', () => {
    const group = filledGroup();
    group.controls.answers.patchValue({ '1': 'false', '2': '8', '3': '  Mejora  ' });
    expect(evaluationPayload(group.getRawValue(), template, metrics)).toEqual({
      segmentId: 3, edema: 1.5, pain: 3, strength: 14, templateId: 5,
      answers: [{ questionId: 1, booleanValue: false }, { questionId: 3, textValue: 'Mejora' }],
      metrics: [{ metricId: 7, numericValue: 130 }],
    });
  });

  it('sends each metric type converted and omits the empty optional ones, never a fixed ROM', () => {
    const group = filledGroup();
    group.controls.metrics.patchValue({ '7': '-10,5', '8': 'false', '9': 'Firme', '10': '  Rígido  ' });
    const payload = evaluationPayload(group.getRawValue(), null, metrics);
    expect(payload.metrics).toEqual([
      { metricId: 7, numericValue: -10.5 },
      { metricId: 8, booleanValue: false },
      { metricId: 9, optionValue: 'Firme' },
      { metricId: 10, textValue: 'Rígido' },
    ]);
    expect('rom' in payload).toBe(false);
    group.controls.metrics.patchValue({ '8': '', '9': '', '10': '   ' });
    expect(evaluationPayload(group.getRawValue(), null, metrics).metrics).toEqual([{ metricId: 7, numericValue: -10.5 }]);
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
        { field: 'evaluations[0].strength', message: 'Fuerza fuera del rango permitido.' },
        { field: 'evaluations[1].pain', message: 'Dolor fuera del rango permitido.' },
        { field: 'date', message: 'otro' },
      ] },
    });
    const errors = metricErrorsByEvaluation(error);
    expect(errors.get(0)).toEqual({ fixed: { strength: 'Fuerza fuera del rango permitido.' }, metrics: {} });
    expect(errors.get(1)).toEqual({ fixed: { pain: 'Dolor fuera del rango permitido.' }, metrics: {} });
    expect(errors.size).toBe(2);
  });

  it('reads the per-metric errors of the backend', () => {
    const error = new HttpErrorResponse({
      status: 422,
      error: { code: 'METRIC_VALUE_REQUIRED', fieldErrors: [
        { field: 'evaluations[0].metrics[7]', message: 'Completá esta métrica.' },
        { field: 'evaluations[0].rom', message: 'ya no existe' },
      ] },
    });
    expect(metricErrorsByEvaluation(error).get(0)).toEqual({ fixed: {}, metrics: { 7: 'Completá esta métrica.' } });
  });
});
