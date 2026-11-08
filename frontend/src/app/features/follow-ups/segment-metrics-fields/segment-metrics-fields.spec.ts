import { TestBed } from '@angular/core/testing';
import { FormControl, FormRecord } from '@angular/forms';
import { provideRouter } from '@angular/router';
import { SegmentMetric } from '../../segment-metrics/segment-metrics.models';
import { resetMetrics } from '../follow-up-form';
import { SegmentMetricsFields } from './segment-metrics-fields';

function metric(partial: Partial<SegmentMetric> & Pick<SegmentMetric, 'id' | 'name' | 'answerType'>): SegmentMetric {
  return { segmentId: 3, required: true, active: true, displayOrder: partial.id, options: [], ...partial };
}

describe('SegmentMetricsFields', () => {
  const metrics: SegmentMetric[] = [
    metric({ id: 7, name: 'Flexión', answerType: 'NUMERIC' }),
    metric({ id: 8, name: 'Pronosupinación completa', answerType: 'YES_NO', required: false }),
    metric({ id: 9, name: 'Sensación final', answerType: 'OPTION', required: false, options: ['Firme', 'Blanda', 'Vacía'] }),
    metric({ id: 10, name: 'Observación', answerType: 'TEXT', required: false }),
  ];

  function render(list: SegmentMetric[], options: { canDefine?: boolean; problems?: Record<number, string> } = {}) {
    TestBed.configureTestingModule({ imports: [SegmentMetricsFields], providers: [provideRouter([])] });
    const values = new FormRecord<FormControl<string>>({});
    resetMetrics(values, list);
    const fixture = TestBed.createComponent(SegmentMetricsFields);
    fixture.componentRef.setInput('metrics', list);
    fixture.componentRef.setInput('values', values);
    fixture.componentRef.setInput('idPrefix', 'evaluation-0');
    fixture.componentRef.setInput('segmentId', 3);
    fixture.componentRef.setInput('canDefine', options.canDefine ?? false);
    fixture.componentRef.setInput('problems', options.problems ?? {});
    fixture.componentRef.setInput('showProblems', true);
    fixture.detectChanges();
    return { el: fixture.nativeElement as HTMLElement, values, fixture };
  }

  it('renders a control per answer type under the "ROM" title', () => {
    const { el } = render(metrics);
    expect(el.querySelector('.rom__title')?.textContent).toContain('ROM');
    expect(el.querySelector<HTMLInputElement>('#evaluation-0-m7')?.getAttribute('inputmode')).toBe('decimal');
    expect(Array.from(el.querySelectorAll('.segmented__option')).map((b) => b.textContent?.trim())).toEqual(['Sí', 'No']);
    expect(Array.from(el.querySelectorAll('.option-chip')).map((b) => b.textContent?.trim()))
      .toEqual(['Firme', 'Blanda', 'Vacía']);
    expect(el.querySelector('textarea#evaluation-0-m10')).not.toBeNull();
  });

  it('marks only the optional metrics with "(opc.)"', () => {
    const { el } = render(metrics);
    const labels = Array.from(el.querySelectorAll('.metric__label'));
    expect(labels.map((label) => label.querySelector('.metric__optional') !== null)).toEqual([false, true, true, true]);
  });

  it('writes typed values and clears the chosen chip when tapped again', () => {
    const { el, values, fixture } = render(metrics);
    const flexion = el.querySelector<HTMLInputElement>('#evaluation-0-m7')!;
    flexion.value = '130';
    flexion.dispatchEvent(new Event('input'));
    el.querySelectorAll<HTMLButtonElement>('.segmented__option')[0].click();
    const firm = el.querySelectorAll<HTMLButtonElement>('.option-chip')[0];
    firm.click();
    fixture.detectChanges();
    expect(values.getRawValue()).toEqual({ '7': '130', '8': 'true', '9': 'Firme', '10': '' });
    expect(firm.getAttribute('aria-pressed')).toBe('true');
    firm.click();
    fixture.detectChanges();
    expect(values.controls['9'].value).toBe('');
    expect(firm.getAttribute('aria-pressed')).toBe('false');
  });

  it('shows the problem under its metric', () => {
    const { el } = render(metrics, { problems: { 7: 'Completá esta métrica.' } });
    expect(el.querySelector('#evaluation-0-m7')?.getAttribute('aria-invalid')).toBe('true');
    expect(el.textContent).toContain('Completá esta métrica.');
  });

  it('says the segment has no metrics, linking to Catálogos only for administrators', () => {
    const { el } = render([]);
    expect(el.textContent).toContain('Este segmento no tiene métricas ROM definidas.');
    expect(el.querySelector('.rom__define')).toBeNull();
    TestBed.resetTestingModule();
    const admin = render([], { canDefine: true }).el;
    const link = admin.querySelector<HTMLAnchorElement>('.rom__define');
    expect(link?.textContent).toContain('Definir en Catálogos');
    expect(link?.getAttribute('href')).toBe('/catalogos/segmentos/3');
  });
});
