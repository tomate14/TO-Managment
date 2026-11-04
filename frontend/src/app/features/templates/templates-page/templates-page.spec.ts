import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { TemplatesPage } from './templates-page';
import { TemplatesService } from '../templates.service';
import { CatalogsService } from '../../catalogs/catalogs.service';
import { TemplateSummary } from '../templates.models';

describe('TemplatesPage', () => {
  const wrist = { id: 1, name: 'Muñeca', active: true };
  const elbow = { id: 2, name: 'Codo', active: true };
  const templates: TemplateSummary[] = [
    { id: 10, name: 'Fractura de muñeca', segment: wrist, active: true, questionCount: 6, chainedCount: 2 },
    { id: 11, name: 'Epicondilitis', segment: elbow, active: false, questionCount: 4, chainedCount: 0 },
  ];
  let list: ReturnType<typeof vi.fn>;
  let catalogList: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    list = vi.fn(() => of(templates));
    catalogList = vi.fn(() => of([wrist, elbow]));
    TestBed.configureTestingModule({
      imports: [TemplatesPage],
      providers: [
        provideRouter([]),
        { provide: TemplatesService, useValue: { list } },
        { provide: CatalogsService, useValue: { list: catalogList } },
      ],
    });
  });

  function render() {
    const fixture = TestBed.createComponent(TemplatesPage);
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  it('shows every template with segment, status and counts', () => {
    const { el } = render();
    const cards = el.querySelectorAll('.template-card');
    expect(cards.length).toBe(2);
    expect(cards[0].textContent).toContain('Fractura de muñeca');
    expect(cards[0].textContent).toContain('Muñeca');
    expect(cards[0].textContent).toContain('Activa');
    expect(cards[0].textContent).toContain('6 preguntas');
    expect(cards[0].textContent).toContain('2 encadenadas');
    expect(cards[1].textContent).toContain('Inactiva');
    expect(cards[0].getAttribute('href')).toBe('/plantillas/10');
  });

  it('offers Todos plus the active segments as filters', () => {
    const { el } = render();
    expect(catalogList).toHaveBeenCalledWith('body-segments', true);
    const chips = Array.from(el.querySelectorAll('.segment-chip')).map((chip) => chip.textContent?.trim());
    expect(chips).toEqual(['Todos', 'Muñeca', 'Codo']);
    expect(el.querySelector('.segment-chip[aria-pressed="true"]')?.textContent).toContain('Todos');
  });

  it('filters by segment', () => {
    const { fixture, el } = render();
    Array.from(el.querySelectorAll<HTMLButtonElement>('.segment-chip')).find((chip) => chip.textContent?.includes('Codo'))!.click();
    fixture.detectChanges();

    const cards = el.querySelectorAll('.template-card');
    expect(cards.length).toBe(1);
    expect(cards[0].textContent).toContain('Epicondilitis');
  });

  it('links to the new-template form', () => {
    const { el } = render();
    expect(el.querySelector('a[href="/plantillas/nueva"]')?.textContent).toContain('Nueva plantilla');
  });

  it('shows an error when templates cannot be loaded', () => {
    list.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));
    const { el } = render();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('No pudimos cargar las plantillas');
  });
});
