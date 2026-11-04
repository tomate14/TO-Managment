import { conditionPreview, conditionSummary, displayExpected, operatorSupports, parseDecimal, typeSummary } from './question-text';
import { TemplateQuestion } from './templates.models';

function question(overrides: Partial<TemplateQuestion>): TemplateQuestion {
  return {
    id: 1, number: 1, depth: 0, displayOrder: 1, text: 'Pregunta', answerType: 'TEXT', minValue: null, maxValue: null,
    options: [], parentQuestionId: null, condition: null, active: true, locked: false, hasDependents: false,
    ...overrides,
  };
}

describe('question text helpers', () => {
  it('summarizes each answer type like the mockup', () => {
    expect(typeSummary(question({ answerType: 'YES_NO' }))).toBe('Sí / No');
    expect(typeSummary(question({ answerType: 'NUMERIC', minValue: 0, maxValue: 24 }))).toBe('Numérica · 0 a 24');
    expect(typeSummary(question({ answerType: 'NUMERIC', minValue: 0 }))).toBe('Numérica · desde 0');
    expect(typeSummary(question({ answerType: 'NUMERIC', maxValue: 10 }))).toBe('Numérica · hasta 10');
    expect(typeSummary(question({ answerType: 'NUMERIC' }))).toBe('Numérica');
    expect(typeSummary(question({ answerType: 'OPTION', options: ['A', 'B', 'C'] }))).toBe('Opción · 3 opciones');
    expect(typeSummary(question({ answerType: 'TEXT' }))).toBe('Texto');
  });

  it('only lets numeric parents use greater/less than', () => {
    expect(operatorSupports('GREATER_THAN', 'NUMERIC')).toBe(true);
    expect(operatorSupports('LESS_THAN', 'YES_NO')).toBe(false);
    expect(operatorSupports('EQUALS', 'OPTION')).toBe(true);
    expect(operatorSupports('NOT_EQUALS', 'TEXT')).toBe(true);
  });

  it('shows yes/no values as Sí / No', () => {
    expect(displayExpected('true', 'YES_NO')).toBe('Sí');
    expect(displayExpected('false', 'YES_NO')).toBe('No');
    expect(displayExpected('Esférico', 'OPTION')).toBe('Esférico');
  });

  it('describes a chained question', () => {
    const parent = question({ id: 1, text: '¿Usa férula?', answerType: 'YES_NO' });
    const child = question({ id: 2, parentQuestionId: 1, condition: { operator: 'EQUALS', expectedValue: 'true' } });
    expect(conditionSummary(child, parent)).toBe('Si «¿Usa férula?» = Sí');
    expect(conditionSummary(parent, undefined)).toBeNull();
    expect(conditionPreview('¿Usa férula?', 'NOT_EQUALS', 'Sí')).toBe('esta pregunta aparece cuando «¿Usa férula?» es distinta de «Sí».');
  });

  it('parses decimals with comma or dot', () => {
    expect(parseDecimal('7,5')).toBe(7.5);
    expect(parseDecimal(' 24 ')).toBe(24);
    expect(parseDecimal('')).toBeNull();
    expect(parseDecimal('abc')).toBeNaN();
  });
});
