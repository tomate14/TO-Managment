import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, ParamMap, Router, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { CatalogsPage } from './catalogs-page';
import { CatalogsService } from '../catalogs.service';
import { CatalogItem } from '../catalogs.models';

describe('CatalogsPage', () => {
  const segments: CatalogItem[] = [
    { id: 1, name: 'Codo', active: true },
    { id: 2, name: 'Dedos', active: false },
    { id: 3, name: 'Muñeca', active: true },
  ];
  const surgeries: CatalogItem[] = [{ id: 10, name: 'Tenorrafia', active: true }];
  let service: Record<'list' | 'create' | 'rename' | 'setActive', ReturnType<typeof vi.fn>>;
  let queryParams: BehaviorSubject<ParamMap>;

  beforeEach(() => {
    queryParams = new BehaviorSubject(convertToParamMap({}));
    service = {
      list: vi.fn((slug: string) => of(slug === 'surgery-types' ? surgeries : segments)),
      create: vi.fn(),
      rename: vi.fn(),
      setActive: vi.fn(),
    };
    TestBed.configureTestingModule({
      imports: [CatalogsPage],
      providers: [
        provideRouter([]),
        { provide: CatalogsService, useValue: service },
        { provide: ActivatedRoute, useValue: { queryParamMap: queryParams.asObservable() } },
      ],
    });
  });

  function render() {
    const fixture = TestBed.createComponent(CatalogsPage);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const type = (selector: string, value: string) => {
      const input = el.querySelector<HTMLInputElement>(selector)!;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    };
    const submit = (selector: string) => {
      el.querySelector(selector)!.dispatchEvent(new Event('submit', { cancelable: true }));
      fixture.detectChanges();
    };
    const row = (name: string) =>
      Array.from(el.querySelectorAll<HTMLElement>('.catalog-row')).find((r) => r.textContent?.includes(name))!;
    return { fixture, el, type, submit, row };
  }

  it('opens Segmentos by default with its items, statuses and the footnote', () => {
    const { el } = render();
    expect(service.list).toHaveBeenCalledWith('body-segments');
    expect(el.querySelector('[aria-pressed="true"]')?.textContent).toContain('Segmentos');
    expect(el.querySelector('label[for="catalog-new"]')?.textContent).toContain('Nuevo segmento');
    expect(el.querySelectorAll('.catalog-row').length).toBe(3);
    expect(el.textContent).toContain('Inactivo');
    expect(el.textContent).toContain('Los inactivos no se ofrecen en altas nuevas');
  });

  it('switches catalog through the ?tipo= query param', () => {
    const { fixture, el } = render();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const surgeriesTab = Array.from(el.querySelectorAll<HTMLButtonElement>('.tabs__tab')).find((b) => b.textContent?.includes('Cirugías'))!;
    surgeriesTab.click();
    expect(navigate).toHaveBeenCalledWith([], expect.objectContaining({ queryParams: { tipo: 'cirugias' } }));

    queryParams.next(convertToParamMap({ tipo: 'cirugias' }));
    fixture.detectChanges();
    expect(service.list).toHaveBeenCalledWith('surgery-types');
    expect(el.querySelector('label[for="catalog-new"]')?.textContent).toContain('Nuevo tipo de cirugía');
    expect(el.textContent).toContain('Tenorrafia');
  });

  it('adds a trimmed item and keeps the list sorted', () => {
    service.create.mockReturnValue(of({ id: 4, name: 'Antebrazo', active: true }));
    const { el, type, submit } = render();
    type('#catalog-new', '  Antebrazo ');
    submit('.catalogs__add');

    expect(service.create).toHaveBeenCalledWith('body-segments', 'Antebrazo');
    const names = Array.from(el.querySelectorAll('.catalog-row__name')).map((n) => n.textContent?.trim());
    expect(names).toEqual(['Antebrazo', 'Codo', 'Dedos', 'Muñeca']);
    expect(el.querySelector<HTMLInputElement>('#catalog-new')?.value).toBe('');
  });

  it('shows the mockup duplicate error under the input', () => {
    service.create.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 409, error: { code: 'CATALOG_ITEM_DUPLICATE', message: 'dup' },
    })));
    const { el, type, submit } = render();
    type('#catalog-new', 'muñeca');
    submit('.catalogs__add');

    expect(el.querySelector('#catalog-new-error')?.textContent).toContain('Ya existe con ese nombre (sin importar mayúsculas).');
    expect(el.querySelector('#catalog-new')?.getAttribute('aria-invalid')).toBe('true');
  });

  it('does not call the backend for a blank name', () => {
    const { el, type, submit } = render();
    type('#catalog-new', '   ');
    submit('.catalogs__add');
    expect(service.create).not.toHaveBeenCalled();
    expect(el.querySelector('#catalog-new-error')?.textContent).toContain('Ingresá un nombre.');
  });

  it('renames an item inline', () => {
    service.rename.mockReturnValue(of({ id: 3, name: 'Muñeca derecha', active: true }));
    const { fixture, el, type, submit, row } = render();
    row('Muñeca').querySelector<HTMLButtonElement>('[aria-label="Editar Muñeca"]')!.click();
    fixture.detectChanges();

    type('#catalog-edit-3', 'Muñeca derecha');
    submit('.catalog-row__edit');

    expect(service.rename).toHaveBeenCalledWith('body-segments', 3, 'Muñeca derecha');
    expect(el.querySelector('#catalog-edit-3')).toBeNull();
    expect(el.textContent).toContain('Muñeca derecha');
  });

  it('cancels an inline edit without saving', () => {
    const { fixture, el, row } = render();
    row('Codo').querySelector<HTMLButtonElement>('[aria-label="Editar Codo"]')!.click();
    fixture.detectChanges();
    el.querySelector<HTMLButtonElement>('.catalog-row__cancel')!.click();
    fixture.detectChanges();

    expect(service.rename).not.toHaveBeenCalled();
    expect(el.querySelector('.catalog-row__edit')).toBeNull();
  });

  it('activates and deactivates with the switch', () => {
    service.setActive.mockReturnValue(of({ id: 2, name: 'Dedos', active: true }));
    const { fixture, row } = render();
    const toggle = row('Dedos').querySelector<HTMLButtonElement>('[role="switch"]')!;
    expect(toggle.getAttribute('aria-checked')).toBe('false');

    toggle.click();
    fixture.detectChanges();

    expect(service.setActive).toHaveBeenCalledWith('body-segments', 2, true);
    expect(row('Dedos').querySelector('[role="switch"]')?.getAttribute('aria-checked')).toBe('true');
  });
});
