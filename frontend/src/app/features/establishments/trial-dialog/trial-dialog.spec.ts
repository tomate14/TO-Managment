import { TestBed } from '@angular/core/testing';
import { TrialDialog } from './trial-dialog';
import { EstablishmentSummary } from '../establishments.models';
import { todayIso } from '../../patients/patient-format';
import { trialLastDay } from '../trial-format';

/** ISO instant of the local midnight `days` days from today (the stored end of a trial). */
function midnightIn(days: number): string {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), now.getDate() + days).toISOString();
}

describe('TrialDialog', () => {
  const lucia: EstablishmentSummary = {
    id: 5,
    name: 'Lucía Fernández',
    kind: 'INDIVIDUAL',
    pendingApproval: false,
    rejected: false,
    createdAt: '2026-11-01T15:00:00Z',
    active: true,
    activeUsers: 1,
    admins: [{ id: 15, fullName: 'Lucía Fernández', email: 'lucia.fernandez@gmail.com' }],
    trialEndsAt: midnightIn(8),
    trialExpired: false,
  };

  function render(establishment: EstablishmentSummary = lucia, desktop = true) {
    TestBed.configureTestingModule({ imports: [TrialDialog] });
    const fixture = TestBed.createComponent(TrialDialog);
    fixture.componentRef.setInput('establishment', establishment);
    fixture.componentRef.setInput('desktop', desktop);
    const saved: string[] = [];
    let removed = 0;
    let cancelled = 0;
    fixture.componentInstance.save.subscribe((date) => saved.push(date));
    fixture.componentInstance.remove.subscribe(() => removed++);
    fixture.componentInstance.cancel.subscribe(() => cancelled++);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    return { fixture, el, saved, removed: () => removed, cancelled: () => cancelled };
  }

  function button(el: HTMLElement, label: string): HTMLButtonElement {
    return Array.from(el.querySelectorAll<HTMLButtonElement>('button')).find((b) => b.textContent?.trim() === label)!;
  }

  function dateInput(el: HTMLElement): HTMLInputElement {
    return el.querySelector<HTMLInputElement>('input[type="date"]')!;
  }

  it('renders the title, the field and the hint as an accessible dialog', () => {
    const { el } = render();
    const dialog = el.querySelector('[role="dialog"]')!;
    expect(dialog.getAttribute('aria-modal')).toBe('true');
    const title = el.querySelector(`#${dialog.getAttribute('aria-labelledby')}`)!;
    expect(title.textContent).toContain('Vencimiento de la prueba');
    expect(el.querySelector('label[for="trial-dialog-date"]')!.textContent).toContain('Último día de prueba');
    expect(el.textContent).toContain('La cuenta se bloquea a la medianoche de ese día.');
    expect(el.textContent).toContain(`Lucía Fernández · prueba hasta el`);
  });

  it('does not let a past day be picked', () => {
    const { el } = render();
    expect(dateInput(el).getAttribute('min')).toBe(todayIso());
  });

  it('is pre-filled with the current last day when it is not in the past', () => {
    const { el } = render();
    expect(dateInput(el).value).toBe(trialLastDay(lucia.trialEndsAt!));
  });

  it('starts empty for an expired trial or one without expiry', () => {
    const { el } = render({ ...lucia, trialEndsAt: midnightIn(-2), trialExpired: true });
    expect(dateInput(el).value).toBe('');
    expect(el.textContent).toContain('Lucía Fernández · venció el');

    TestBed.resetTestingModule();
    const { el: none } = render({ ...lucia, trialEndsAt: null });
    expect(dateInput(none).value).toBe('');
    expect(none.textContent).toContain('Lucía Fernández · sin vencimiento');
  });

  it('asks for a date before saving', () => {
    const { fixture, el, saved } = render({ ...lucia, trialEndsAt: null });
    button(el, 'Guardar').click();
    fixture.detectChanges();
    expect(el.querySelector('.field__error')!.textContent).toContain('Elegí una fecha.');
    expect(saved).toEqual([]);
  });

  it('emits the chosen day', () => {
    const { el, saved } = render();
    const input = dateInput(el);
    input.value = '2099-11-30';
    input.dispatchEvent(new Event('input'));
    button(el, 'Guardar').click();
    expect(saved).toEqual(['2099-11-30']);
  });

  it('emits remove and cancel', () => {
    const { el, removed, cancelled } = render();
    button(el, 'Quitar vencimiento').click();
    expect(removed()).toBe(1);
    button(el, 'Cancelar').click();
    expect(cancelled()).toBe(1);
    el.querySelector<HTMLElement>('.trial-overlay')!.click();
    expect(cancelled()).toBe(2);
  });

  it('shows the error under the field', () => {
    const { fixture, el } = render();
    fixture.componentRef.setInput('error', 'La fecha de vencimiento no puede ser anterior a hoy.');
    fixture.detectChanges();
    const error = el.querySelector('.field__error')!;
    expect(error.textContent).toContain('La fecha de vencimiento no puede ser anterior a hoy.');
    expect(dateInput(el).classList).toContain('field__input--invalid');
    expect(dateInput(el).getAttribute('aria-describedby')).toContain(error.id);
  });

  it('is a bottom sheet with the paid-account hint on mobile', () => {
    const { el } = render(lucia, false);
    expect(el.querySelector('.trial-dialog--sheet')).not.toBeNull();
    expect(el.textContent).toContain('Si es una cuenta paga, quitá el vencimiento.');
    expect(button(el, 'Guardar')).toBeTruthy();
    expect(button(el, 'Quitar vencimiento')).toBeTruthy();
    expect(button(el, 'Cancelar')).toBeTruthy();
  });

  it('disables the buttons while busy', () => {
    const { fixture, el } = render();
    fixture.componentRef.setInput('busy', true);
    fixture.detectChanges();
    expect(button(el, 'Guardar').disabled).toBe(true);
    expect(button(el, 'Quitar vencimiento').disabled).toBe(true);
  });
});
