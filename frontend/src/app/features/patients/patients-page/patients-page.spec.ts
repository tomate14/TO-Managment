import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { PatientsPage } from './patients-page';
import { PatientsService } from '../patients.service';
import { AuthService } from '../../../core/auth/auth.service';
import { PageResponse, Patient } from '../patients.models';

function patient(id: number, firstName: string, lastName: string, dni: string): Patient {
  return {
    id, dni, firstName, lastName, birthDate: '1984-03-21', phone: '249 412-3456', occupation: 'Docente',
    healthInsurance: 'OSDE', healthInsuranceNumber: '1', dominance: id % 2 === 0 ? 'LEFT' : 'RIGHT',
  };
}

function page(content: Patient[], index: number, totalPages: number, totalElements = content.length): PageResponse<Patient> {
  return { content, page: index, size: 20, totalElements, totalPages };
}

describe('PatientsPage', () => {
  const castro = patient(1, 'Ricardo', 'Castro', '22410876');
  const fernandez = patient(2, 'Lucía', 'Fernández', '30123456');
  const lopez = patient(3, 'Agustina', 'López', '41276590');
  let search: ReturnType<typeof vi.fn>;

  function setup(desktop: boolean) {
    vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: desktop, addEventListener: vi.fn(), removeEventListener: vi.fn() })));
    TestBed.configureTestingModule({
      imports: [PatientsPage],
      providers: [
        provideRouter([]),
        { provide: PatientsService, useValue: { search } },
        { provide: AuthService, useValue: { currentUser: signal({ id: 1, email: 'm@c.com', fullName: 'Mariana Gómez', role: 'ADMIN' }) } },
      ],
    });
    const fixture = TestBed.createComponent(PatientsPage);
    fixture.detectChanges();
    TestBed.flushEffects();
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  beforeEach(() => {
    search = vi.fn(() => of(page([castro, fernandez], 0, 2, 3)));
  });

  afterEach(() => vi.unstubAllGlobals());

  it('lists patients by last name with DNI and health insurance on mobile', () => {
    const { el } = setup(false);
    expect(search).toHaveBeenCalledWith('', 0);
    const rows = el.querySelectorAll('.patient-row');
    expect(rows.length).toBe(2);
    expect(rows[1].textContent).toContain('Fernández, Lucía');
    expect(rows[1].textContent).toContain('DNI 30.123.456 · OSDE');
    expect(rows[1].getAttribute('href')).toBe('/pacientes/2');
    expect(el.textContent).toContain('Ordenados por apellido');
  });

  it('appends the next page with "Cargar más"', () => {
    const { fixture, el } = setup(false);
    search.mockReturnValue(of(page([lopez], 1, 2, 3)));
    el.querySelector<HTMLButtonElement>('.patients__more')!.click();
    fixture.detectChanges();

    expect(search).toHaveBeenLastCalledWith('', 1);
    expect(el.querySelectorAll('.patient-row').length).toBe(3);
    expect(el.querySelector('.patients__more')).toBeNull();
  });

  it('searches after a short pause and shows the result count', async () => {
    vi.useFakeTimers();
    const { fixture, el } = setup(false);
    search.mockReturnValue(of(page([fernandez], 0, 1)));
    const input = el.querySelector<HTMLInputElement>('#patient-search')!;
    input.value = 'fernán';
    input.dispatchEvent(new Event('input'));
    vi.advanceTimersByTime(1000);
    fixture.detectChanges();
    vi.useRealTimers();

    expect(search).toHaveBeenLastCalledWith('fernán', 0);
    expect(el.textContent).toContain('1 resultados');
    expect(el.querySelectorAll('.patient-row').length).toBe(1);
  });

  describe('search timing', () => {
    const type = (el: HTMLElement, text: string) => {
      const input = el.querySelector<HTMLInputElement>('#patient-search')!;
      input.value = text;
      input.dispatchEvent(new Event('input'));
    };
    const pressEnter = (el: HTMLElement) => {
      const event = new KeyboardEvent('keydown', { key: 'Enter', cancelable: true, bubbles: true });
      el.querySelector<HTMLInputElement>('#patient-search')!.dispatchEvent(event);
      return event;
    };
    beforeEach(() => vi.useFakeTimers());
    afterEach(() => vi.useRealTimers());

    it('does not request before 1000 ms without keystrokes, and requests after', () => {
      const { el } = setup(false);
      search.mockClear();
      type(el, 'fer');
      vi.advanceTimersByTime(999);
      expect(search).not.toHaveBeenCalled();
      type(el, 'fern');
      vi.advanceTimersByTime(999);
      expect(search).not.toHaveBeenCalled();
      vi.advanceTimersByTime(1);
      expect(search).toHaveBeenCalledTimes(1);
      expect(search).toHaveBeenCalledWith('fern', 0);
    });

    it('requests immediately on Enter and prevents the default', () => {
      const { el } = setup(false);
      search.mockClear();
      type(el, 'fernán');
      const event = pressEnter(el);
      expect(event.defaultPrevented).toBe(true);
      expect(search).toHaveBeenCalledTimes(1);
      expect(search).toHaveBeenCalledWith('fernán', 0);
    });

    it('does not request again when the debounce fires after Enter with the same text', () => {
      const { el } = setup(false);
      search.mockClear();
      type(el, 'fernán');
      pressEnter(el);
      vi.advanceTimersByTime(1000);
      expect(search).toHaveBeenCalledTimes(1);
    });
  });

  it('explains when a search finds nobody', async () => {
    vi.useFakeTimers();
    const { fixture, el } = setup(false);
    search.mockReturnValue(of(page([], 0, 0)));
    const input = el.querySelector<HTMLInputElement>('#patient-search')!;
    input.value = 'zzz';
    input.dispatchEvent(new Event('input'));
    vi.advanceTimersByTime(1000);
    fixture.detectChanges();
    vi.useRealTimers();

    expect(el.textContent).toContain('No encontramos pacientes con esa búsqueda.');
  });

  it('shows a table with pages on desktop', () => {
    const { fixture, el } = setup(true);
    const rows = el.querySelectorAll('.patients-table tbody tr');
    expect(rows.length).toBe(2);
    expect(rows[1].textContent).toContain('30.123.456');
    expect(rows[1].textContent).toContain('Zurdo');
    expect(el.textContent).toContain('Página 1');

    search.mockReturnValue(of(page([lopez], 1, 2, 3)));
    Array.from(el.querySelectorAll<HTMLButtonElement>('.pager__button')).find((b) => b.textContent?.includes('Siguiente'))!.click();
    fixture.detectChanges();

    expect(search).toHaveBeenLastCalledWith('', 1);
    expect(el.querySelectorAll('.patients-table tbody tr').length).toBe(1);
    expect(el.textContent).toContain('Página 2');
  });

  it('links to the new-patient form', () => {
    const { el } = setup(false);
    expect(el.querySelector('a.patients__fab')?.getAttribute('href')).toBe('/pacientes/nuevo');
  });

  it('shows an error when patients cannot be loaded', () => {
    search.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));
    const { el } = setup(false);
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('No pudimos cargar los pacientes');
  });
});
