import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { SegmentMetricForm } from './segment-metric-form';
import { SegmentMetricsService } from '../segment-metrics.service';
import { SegmentMetric } from '../segment-metrics.models';

describe('SegmentMetricForm', () => {
  const flexion: SegmentMetric = {
    id: 8, segmentId: 3, name: 'Flexión', answerType: 'NUMERIC', required: true, active: true, displayOrder: 1, options: [],
  };
  let service: Record<'create' | 'update', ReturnType<typeof vi.fn>>;

  function setup(metric: SegmentMetric | null) {
    service = { create: vi.fn(), update: vi.fn() };
    TestBed.configureTestingModule({
      imports: [SegmentMetricForm],
      providers: [{ provide: SegmentMetricsService, useValue: service }],
    });
    const fixture = TestBed.createComponent(SegmentMetricForm);
    fixture.componentRef.setInput('segmentId', 3);
    fixture.componentRef.setInput('metric', metric);
    const saved = vi.fn();
    const cancelled = vi.fn();
    fixture.componentInstance.saved.subscribe(saved);
    fixture.componentInstance.cancelled.subscribe(cancelled);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const type = (selector: string, value: string) => {
      const input = el.querySelector<HTMLInputElement>(selector)!;
      input.value = value;
      input.dispatchEvent(new Event('input'));
      fixture.detectChanges();
    };
    const chip = (label: string) => {
      Array.from(el.querySelectorAll<HTMLButtonElement>('.type-chip')).find((b) => b.textContent?.trim() === label)!.click();
      fixture.detectChanges();
    };
    const submit = () => {
      el.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
      fixture.detectChanges();
    };
    return { fixture, el, type, chip, submit, saved, cancelled };
  }

  it('asks for the name when it is blank', () => {
    const { el, type, submit } = setup(null);
    type('#metric-name', '   ');
    submit();
    expect(service.create).not.toHaveBeenCalled();
    expect(el.querySelector('#metric-name-error')?.textContent).toContain('Ingresá el nombre de la métrica.');
  });

  it('asks for at least 2 options on an Opción metric', () => {
    const { el, type, chip, submit } = setup(null);
    type('#metric-name', 'Sensación final');
    chip('Opción');
    type('[aria-label="Opción 1"]', 'Firme');
    submit();
    expect(service.create).not.toHaveBeenCalled();
    expect(el.querySelector('#metric-options-error')?.textContent).toContain('Agregá al menos 2 opciones.');
  });

  it('rejects options repeated ignoring case', () => {
    const { el, type, chip, submit } = setup(null);
    type('#metric-name', 'Sensación final');
    chip('Opción');
    type('[aria-label="Opción 1"]', 'Firme');
    type('[aria-label="Opción 2"]', 'firme');
    submit();
    expect(service.create).not.toHaveBeenCalled();
    expect(el.querySelector('#metric-options-error')?.textContent).toContain('Las opciones no se pueden repetir.');
  });

  it('creates an Opción metric with trimmed values and emits saved', () => {
    const created: SegmentMetric = {
      ...flexion, id: 9, name: 'Sensación final', answerType: 'OPTION', required: false, options: ['Firme', 'Blanda'],
    };
    const { el, type, chip, submit, saved } = setup(null);
    service.create.mockReturnValue(of(created));
    expect(el.querySelector('.metric-form__submit')?.textContent).toContain('Agregar métrica');
    type('#metric-name', ' Sensación final ');
    chip('Opción');
    type('[aria-label="Opción 1"]', 'Firme ');
    type('[aria-label="Opción 2"]', 'Blanda');
    submit();
    expect(service.create).toHaveBeenCalledWith(3, {
      name: 'Sensación final', answerType: 'OPTION', required: false, options: ['Firme', 'Blanda'],
    });
    expect(saved).toHaveBeenCalledWith(created);
  });

  it('shows the type locked when editing and leaves answerType out of the payload', () => {
    const updated = { ...flexion, name: 'Flexión activa', required: false };
    const { el, type, submit, saved } = setup(flexion);
    service.update.mockReturnValue(of(updated));
    expect(el.querySelectorAll('.type-chip').length).toBe(0);
    expect(el.querySelector<HTMLInputElement>('#metric-type')?.value).toBe('Numérica');
    expect(el.querySelector<HTMLInputElement>('#metric-type')?.readOnly).toBe(true);
    expect(el.textContent).toContain('El tipo no se puede cambiar.');
    expect(el.querySelector('[role="switch"]')?.getAttribute('aria-checked')).toBe('true');
    expect(el.querySelector('.metric-form__submit')?.textContent).toContain('Guardar cambios');

    type('#metric-name', 'Flexión activa');
    el.querySelector<HTMLButtonElement>('[role="switch"]')!.click();
    submit();

    expect(service.update).toHaveBeenCalledWith(3, 8, { name: 'Flexión activa', required: false, options: [] });
    expect(service.update.mock.calls[0][2]).not.toHaveProperty('answerType');
    expect(saved).toHaveBeenCalledWith(updated);
  });

  it('shows the taken-name error on the name field', () => {
    const { el, type, submit, saved } = setup(null);
    service.create.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 409, error: { code: 'SEGMENT_METRIC_NAME_TAKEN', message: 'dup' },
    })));
    type('#metric-name', 'Flexión');
    submit();
    expect(el.querySelector('#metric-name-error')?.textContent).toContain('Ya hay una métrica con ese nombre en este segmento.');
    expect(el.querySelector('#metric-name')?.getAttribute('aria-invalid')).toBe('true');
    expect(saved).not.toHaveBeenCalled();
  });

  it('emits cancelled from Cancelar', () => {
    const { el, cancelled } = setup(null);
    el.querySelector<HTMLButtonElement>('.metric-form__cancel')!.click();
    expect(cancelled).toHaveBeenCalled();
  });
});
