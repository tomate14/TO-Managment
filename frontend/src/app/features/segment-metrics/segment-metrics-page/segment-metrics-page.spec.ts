import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { SegmentMetricsPage } from './segment-metrics-page';
import { SegmentMetricsService } from '../segment-metrics.service';
import { SegmentMetric } from '../segment-metrics.models';
import { CatalogsService } from '../../catalogs/catalogs.service';

function metric(overrides: Partial<SegmentMetric>): SegmentMetric {
  return { id: 1, segmentId: 3, name: 'Métrica', answerType: 'NUMERIC', required: false, active: true, displayOrder: 1, options: [], ...overrides };
}

describe('SegmentMetricsPage', () => {
  const flexion = metric({ id: 1, name: 'Flexión', required: true, displayOrder: 1 });
  const sensacion = metric({ id: 2, name: 'Sensación final', answerType: 'OPTION', options: ['Firme', 'Blanda', 'Vacía'], displayOrder: 2 });
  const hiper = metric({ id: 3, name: 'Hiperextensión', active: false, displayOrder: 3 });
  let service: Record<'list' | 'create' | 'update' | 'setActive' | 'reorder', ReturnType<typeof vi.fn>>;

  afterEach(() => vi.unstubAllGlobals());

  function setup(list: SegmentMetric[], desktop = false) {
    vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: desktop, addEventListener: vi.fn(), removeEventListener: vi.fn() })));
    service = { list: vi.fn(() => of(list)), create: vi.fn(), update: vi.fn(), setActive: vi.fn(), reorder: vi.fn() };
    TestBed.configureTestingModule({
      imports: [SegmentMetricsPage],
      providers: [
        provideRouter([]),
        { provide: SegmentMetricsService, useValue: service },
        { provide: CatalogsService, useValue: { list: vi.fn(() => of([{ id: 3, name: 'Codo', active: true }])) } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ segmentId: '3' }) } } },
      ],
    });
    const fixture = TestBed.createComponent(SegmentMetricsPage);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const button = (label: string) => el.querySelector<HTMLButtonElement>(`[aria-label="${label}"]`)!;
    const names = () => Array.from(el.querySelectorAll('.metric-row__name')).map((n) => n.textContent?.trim());
    return { fixture, el, button, names };
  }

  it('shows the empty state for a segment without metrics', () => {
    const { el } = setup([]);
    expect(service.list).toHaveBeenCalledWith(3);
    expect(el.textContent).toContain('Todavía no hay métricas ROM para este segmento');
    expect(el.textContent).toContain('Segmento: Codo');
    expect(el.querySelector('.metrics-empty__button')?.textContent).toContain('Nueva métrica');
  });

  it('lists the metrics with type, required and inactive chips', () => {
    const { el, names } = setup([flexion, sensacion, hiper]);
    expect(names()).toEqual(['Flexión', 'Sensación final', 'Hiperextensión']);
    const rows = el.querySelectorAll<HTMLElement>('.metric-row');
    expect(rows[0].textContent).toContain('Numérica');
    expect(rows[0].textContent).toContain('Obligatoria');
    expect(rows[1].textContent).toContain('Opción · 3');
    expect(rows[1].textContent).not.toContain('Obligatoria');
    expect(rows[2].textContent).toContain('Inactiva');
    expect(el.textContent).toContain('Las inactivas no se piden en seguimientos nuevos');
  });

  it('moves a metric down through reorder and disables moving the first one up', () => {
    const { fixture, button, names } = setup([flexion, sensacion, hiper]);
    service.reorder.mockReturnValue(of([sensacion, flexion, hiper]));
    expect(button('Subir Flexión').disabled).toBe(true);
    expect(button('Bajar Hiperextensión').disabled).toBe(true);

    button('Bajar Flexión').click();
    fixture.detectChanges();

    expect(service.reorder).toHaveBeenCalledWith(3, [2, 1, 3]);
    expect(names()).toEqual(['Sensación final', 'Flexión', 'Hiperextensión']);
  });

  it('restores the order and shows the error when reorder fails', () => {
    const { fixture, el, button, names } = setup([flexion, sensacion, hiper]);
    service.reorder.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 422, error: { code: 'SEGMENT_METRIC_ORDER_INVALID', message: 'El orden tiene que incluir todas las métricas del segmento.' },
    })));
    button('Subir Hiperextensión').click();
    fixture.detectChanges();

    expect(names()).toEqual(['Flexión', 'Sensación final', 'Hiperextensión']);
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('El orden tiene que incluir todas las métricas del segmento.');
  });

  it('activates and deactivates with the switch', () => {
    const { fixture, button } = setup([flexion, hiper]);
    service.setActive.mockReturnValue(of({ ...hiper, active: true }));
    expect(button('Activa: Hiperextensión').getAttribute('aria-checked')).toBe('false');

    button('Activa: Hiperextensión').click();
    fixture.detectChanges();

    expect(service.setActive).toHaveBeenCalledWith(3, 3, true);
    expect(button('Activa: Hiperextensión').getAttribute('aria-checked')).toBe('true');
  });

  it('opens the side panel for a new metric on desktop', () => {
    const { fixture, el } = setup([flexion], true);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    el.querySelector<HTMLButtonElement>('.metrics-header__new')!.click();
    fixture.detectChanges();

    const panel = el.querySelector('[role="dialog"]');
    expect(navigate).not.toHaveBeenCalled();
    expect(panel?.getAttribute('aria-modal')).toBe('true');
    expect(panel?.getAttribute('aria-labelledby')).toBe('metric-panel-title');
    expect(el.querySelector('#metric-panel-title')?.textContent).toContain('Nueva métrica');
    expect(panel?.querySelector('app-segment-metric-form')).toBeTruthy();
  });

  it('adds the saved metric to the list and closes the panel on desktop', () => {
    const created = metric({ id: 9, name: 'Extensión', displayOrder: 2 });
    const { fixture, el, names } = setup([flexion], true);
    service.create.mockReturnValue(of(created));
    el.querySelector<HTMLButtonElement>('.metrics-header__new')!.click();
    fixture.detectChanges();
    const input = el.querySelector<HTMLInputElement>('#metric-name')!;
    input.value = 'Extensión';
    input.dispatchEvent(new Event('input'));
    el.querySelector('.metric-form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();

    expect(el.querySelector('[role="dialog"]')).toBeNull();
    expect(names()).toEqual(['Flexión', 'Extensión']);
  });

  it('navigates to the new-metric page on mobile', () => {
    const { el } = setup([flexion]);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    el.querySelector<HTMLButtonElement>('.metrics-header__new')!.click();
    expect(navigate).toHaveBeenCalledWith(['/catalogos/segmentos', 3, 'metricas', 'nueva']);
    expect(el.querySelector('[role="dialog"]')).toBeNull();
  });

  it('navigates to the edit page from the pencil on mobile', () => {
    const { button } = setup([flexion]);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    button('Editar Flexión').click();
    expect(navigate).toHaveBeenCalledWith(['/catalogos/segmentos', 3, 'metricas', 1]);
  });
});
