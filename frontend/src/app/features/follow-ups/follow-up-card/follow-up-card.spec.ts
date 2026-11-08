import { TestBed } from '@angular/core/testing';
import { FollowUp, FollowUpMetricValue, SegmentEvaluation } from '../follow-ups.models';
import { FollowUpCard } from './follow-up-card';

function value(partial: Partial<FollowUpMetricValue> & Pick<FollowUpMetricValue, 'metricId' | 'name' | 'answerType'>): FollowUpMetricValue {
  return { numericValue: null, booleanValue: null, optionValue: null, textValue: null, ...partial };
}

describe('FollowUpCard', () => {
  const base: SegmentEvaluation = {
    id: 11, segment: { id: 3, name: 'Codo', active: true }, edema: 1.5, pain: 3, rom: null, strength: 14,
    template: null, answers: [], metrics: [],
  };

  function render(evaluation: SegmentEvaluation, billed = false) {
    const followUp: FollowUp = { id: 9, diagnosisId: 3, date: '2026-11-07', scarType: null, evaluations: [evaluation], billed };
    const fixture = TestBed.createComponent(FollowUpCard);
    fixture.componentRef.setInput('followUp', followUp);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  function tiles(el: HTMLElement): string[] {
    return Array.from(el.querySelectorAll('.segment__metric'))
      .map((tile) => `${tile.querySelector('dt')?.textContent?.trim()} ${tile.querySelector('dd')?.textContent?.trim()}`);
  }

  it('shows the ROM block as "Nombre: valor" for follow-ups with ROM metrics', () => {
    const el = render({
      ...base,
      metrics: [
        value({ metricId: 7, name: 'Flexión', answerType: 'NUMERIC', numericValue: 130.5 }),
        value({ metricId: 8, name: 'Extensión', answerType: 'NUMERIC', numericValue: -10 }),
        value({ metricId: 9, name: 'Pronosupinación completa', answerType: 'YES_NO', booleanValue: true }),
        value({ metricId: 10, name: 'Dolor al final', answerType: 'YES_NO', booleanValue: false }),
        value({ metricId: 11, name: 'Sensación final', answerType: 'OPTION', optionValue: 'Firme' }),
      ],
    });
    expect(tiles(el)).toEqual(['Edema 1,5', 'Dolor 3', 'Fuerza 14']);
    expect(el.querySelector('.segment__rom-title')?.textContent).toContain('ROM');
    const rows = Array.from(el.querySelectorAll('.segment__rom-value')).map((row) => row.textContent?.replace(/\s+/g, ' ').trim());
    expect(rows).toEqual([
      'Flexión: 130,5', 'Extensión: -10', 'Pronosupinación completa: Sí', 'Dolor al final: No', 'Sensación final: Firme',
    ]);
  });

  it('shows the fixed ROM tile of older follow-ups, without a ROM block', () => {
    const el = render({ ...base, rom: 90 });
    expect(tiles(el)).toEqual(['Edema 1,5', 'Dolor 3', 'ROM 90', 'Fuerza 14']);
    expect(el.querySelector('.segment__rom')).toBeNull();
  });

  it('shows the "Facturada" badge only when the follow-up is billed', () => {
    expect(render(base, true).querySelector('.follow-up__billed')?.textContent?.trim()).toBe('Facturada');
    TestBed.resetTestingModule();
    expect(render(base, false).querySelector('.follow-up__billed')).toBeNull();
  });
});
