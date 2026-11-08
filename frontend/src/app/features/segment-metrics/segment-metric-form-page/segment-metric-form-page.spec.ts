import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { SegmentMetricFormPage } from './segment-metric-form-page';
import { SegmentMetricsService } from '../segment-metrics.service';
import { SegmentMetric } from '../segment-metrics.models';
import { CatalogsService } from '../../catalogs/catalogs.service';

describe('SegmentMetricFormPage', () => {
  const flexion: SegmentMetric = {
    id: 8, segmentId: 3, name: 'Flexión', answerType: 'NUMERIC', required: true, active: true, displayOrder: 1, options: [],
  };
  let service: Record<'list' | 'create' | 'update', ReturnType<typeof vi.fn>>;

  function setup(params: Record<string, string>) {
    service = { list: vi.fn(() => of([flexion])), create: vi.fn(), update: vi.fn() };
    TestBed.configureTestingModule({
      imports: [SegmentMetricFormPage],
      providers: [
        provideRouter([]),
        { provide: SegmentMetricsService, useValue: service },
        { provide: CatalogsService, useValue: { list: vi.fn(() => of([{ id: 3, name: 'Codo', active: true }])) } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap(params) } } },
      ],
    });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(SegmentMetricFormPage);
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement, navigate };
  }

  it('shows an empty form under "Nueva métrica" with the segment name', () => {
    const { el } = setup({ segmentId: '3' });
    expect(el.querySelector('h1')?.textContent).toContain('Nueva métrica');
    expect(el.textContent).toContain('Segmento: Codo');
    expect(el.querySelector<HTMLInputElement>('#metric-name')?.value).toBe('');
    expect(service.list).not.toHaveBeenCalled();
  });

  it('loads the metric to edit', () => {
    const { el } = setup({ segmentId: '3', metricId: '8' });
    expect(el.querySelector('h1')?.textContent).toContain('Editar métrica');
    expect(service.list).toHaveBeenCalledWith(3);
    expect(el.querySelector<HTMLInputElement>('#metric-name')?.value).toBe('Flexión');
  });

  it('says so when the metric does not exist', () => {
    const { el } = setup({ segmentId: '3', metricId: '99' });
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('La métrica no existe.');
    expect(el.querySelector('app-segment-metric-form')).toBeNull();
  });

  it('returns to the metrics list when cancelled', () => {
    const { el, navigate } = setup({ segmentId: '3' });
    el.querySelector<HTMLButtonElement>('.metric-form__cancel')!.click();
    expect(navigate).toHaveBeenCalledWith(['/catalogos/segmentos', 3]);
  });
});
