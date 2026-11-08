import { vi } from 'vitest';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { SegmentMetric } from '../../segment-metrics/segment-metrics.models';
import { SegmentMetricsService } from '../../segment-metrics/segment-metrics.service';
import { TemplatesService } from '../../templates/templates.service';
import { createEvaluationGroup } from '../follow-up-form';
import { SegmentEvaluationEditor } from './segment-evaluation-editor';

function metric(partial: Partial<SegmentMetric> & Pick<SegmentMetric, 'id' | 'segmentId' | 'name'>): SegmentMetric {
  return { answerType: 'NUMERIC', required: true, active: true, displayOrder: partial.id, options: [], ...partial };
}

describe('SegmentEvaluationEditor', () => {
  const metricsBySegment: Record<number, SegmentMetric[]> = {
    3: [metric({ id: 7, segmentId: 3, name: 'Flexión' }), metric({ id: 8, segmentId: 3, name: 'Vieja', active: false })],
    4: [metric({ id: 20, segmentId: 4, name: 'Desviación radial' })],
  };
  let list: ReturnType<typeof vi.fn>;

  function render() {
    list = vi.fn((segmentId: number) => of(metricsBySegment[segmentId] ?? []));
    TestBed.configureTestingModule({
      imports: [SegmentEvaluationEditor],
      providers: [
        provideRouter([]),
        { provide: SegmentMetricsService, useValue: { list } },
        { provide: TemplatesService, useValue: { listForSegment: vi.fn(() => of([])), get: vi.fn() } },
        { provide: AuthService, useValue: { isAdminInEstablishment: signal(false) } },
      ],
    });
    const group = createEvaluationGroup();
    const fixture = TestBed.createComponent(SegmentEvaluationEditor);
    fixture.componentRef.setInput('group', group);
    fixture.componentRef.setInput('index', 0);
    fixture.componentRef.setInput('segments', [
      { id: 3, name: 'Codo', active: true }, { id: 4, name: 'Muñeca', active: true }]);
    const emitted: SegmentMetric[][] = [];
    fixture.componentInstance.metricsChange.subscribe((metrics) => emitted.push(metrics));
    fixture.detectChanges();
    return { el: fixture.nativeElement as HTMLElement, group, fixture, emitted };
  }

  it('has no fixed ROM field anymore: only Edema and Fuerza next to the pain', () => {
    const { el } = render();
    expect(el.querySelector('#evaluation-0-rom')).toBeNull();
    expect(el.querySelector('#evaluation-0-edema')).not.toBeNull();
    expect(el.querySelector('#evaluation-0-strength')).not.toBeNull();
  });

  it('loads the active metrics of the chosen segment and emits them', () => {
    const { el, group, fixture, emitted } = render();
    group.controls.segmentId.setValue(3);
    fixture.detectChanges();
    expect(list).toHaveBeenCalledWith(3);
    expect(emitted.at(-1)?.map((m) => m.id)).toEqual([7]);
    expect(Object.keys(group.controls.metrics.controls)).toEqual(['7']);
    expect(el.querySelector('#evaluation-0-m7')).not.toBeNull();
  });

  it('drops the values typed for the previous segment when the segment changes', () => {
    const { group, fixture, emitted } = render();
    group.controls.segmentId.setValue(3);
    group.controls.metrics.controls['7'].setValue('130');
    group.controls.segmentId.setValue(4);
    fixture.detectChanges();
    expect(list).toHaveBeenLastCalledWith(4);
    expect(group.controls.metrics.getRawValue()).toEqual({ '20': '' });
    expect(emitted.at(-1)?.map((m) => m.id)).toEqual([20]);
  });
});
