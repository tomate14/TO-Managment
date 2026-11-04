import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { TemplateEditPage } from './template-edit-page';
import { TemplatesService } from '../templates.service';
import { CatalogsService } from '../../catalogs/catalogs.service';
import { TemplateDetail, TemplateQuestion } from '../templates.models';

function question(overrides: Partial<TemplateQuestion>): TemplateQuestion {
  return {
    id: 1, number: 1, depth: 0, displayOrder: 1, text: 'Pregunta', answerType: 'TEXT', minValue: null, maxValue: null,
    options: [], parentQuestionId: null, condition: null, active: true, locked: false, hasDependents: false,
    ...overrides,
  };
}

describe('TemplateEditPage', () => {
  const wrist = { id: 1, name: 'Muñeca', active: true };
  const detail: TemplateDetail = {
    id: 10, name: 'Fractura de muñeca', segment: wrist, active: true,
    questions: [
      question({ id: 1, number: 1, text: '¿Usa férula?', answerType: 'YES_NO', locked: true, hasDependents: true }),
      question({ id: 2, number: 2, depth: 1, displayOrder: 3, text: 'Horas de uso por día', answerType: 'NUMERIC',
        minValue: 0, maxValue: 24, parentQuestionId: 1, condition: { operator: 'EQUALS', expectedValue: 'true' } }),
      question({ id: 3, number: 3, displayOrder: 2, text: 'Observaciones' }),
    ],
  };
  let service: Record<'get' | 'create' | 'update' | 'setActive' | 'setQuestionActive', ReturnType<typeof vi.fn>>;

  function setup(id: string | null) {
    service = {
      get: vi.fn(() => of(detail)),
      create: vi.fn(() => of({ ...detail, id: 42, questions: [] })),
      update: vi.fn(() => of(detail)),
      setActive: vi.fn(() => of({ ...detail, active: false })),
      setQuestionActive: vi.fn(),
    };
    TestBed.configureTestingModule({
      imports: [TemplateEditPage],
      providers: [
        provideRouter([]),
        { provide: TemplatesService, useValue: service },
        { provide: CatalogsService, useValue: { list: vi.fn(() => of([wrist, { id: 2, name: 'Codo', active: true }])) } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap(id === null ? {} : { id }) } } },
      ],
    });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(TemplateEditPage);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const submit = () => {
      el.querySelector('form')!.dispatchEvent(new Event('submit'));
      fixture.detectChanges();
    };
    return { fixture, el, navigate, submit };
  }

  it('creates a template and continues to its editor', () => {
    const { el, navigate, submit } = setup(null);
    expect(el.querySelector('h1')?.textContent).toContain('Nueva plantilla');
    expect(el.querySelector('.questions')).toBeNull();

    const name = el.querySelector<HTMLInputElement>('#template-name')!;
    name.value = ' Epicondilitis ';
    name.dispatchEvent(new Event('input'));
    const segment = el.querySelector<HTMLSelectElement>('#template-segment')!;
    segment.value = segment.options[2].value;
    segment.dispatchEvent(new Event('change'));
    submit();

    expect(service.create).toHaveBeenCalledWith({ name: 'Epicondilitis', segmentId: 2 });
    expect(navigate).toHaveBeenCalledWith(['/plantillas', 42], { replaceUrl: true });
  });

  it('requires name and segment', () => {
    const { el, submit } = setup(null);
    submit();
    expect(service.create).not.toHaveBeenCalled();
    expect(el.textContent).toContain('Ingresá el nombre.');
    expect(el.textContent).toContain('Elegí un segmento.');
  });

  it('shows a duplicate name under the name field', () => {
    const { fixture, el, submit } = setup('10');
    service.update.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 409, error: { code: 'TEMPLATE_DUPLICATE', message: 'dup' },
    })));
    submit();
    fixture.detectChanges();
    expect(el.querySelector('#template-name-error')?.textContent).toContain('Ya hay una plantilla con ese nombre en el segmento.');
  });

  it('loads the template with its questions in tree order, indenting chained ones', () => {
    const { el } = setup('10');
    expect(el.querySelector<HTMLInputElement>('#template-name')?.value).toBe('Fractura de muñeca');
    const cards = el.querySelectorAll<HTMLElement>('.question-card');
    expect(cards.length).toBe(3);
    expect(cards[1].textContent).toContain('Horas de uso por día');
    expect(cards[1].textContent).toContain('Numérica · 0 a 24');
    expect(cards[1].textContent).toContain('Si «¿Usa férula?» = Sí');
    expect(cards[1].style.marginLeft).toBe('20px');
    expect(cards[0].style.marginLeft).toBe('0px');
  });

  it('locks questions with answers behind a switch and lets the rest be edited', () => {
    const { el } = setup('10');
    const cards = el.querySelectorAll<HTMLElement>('.question-card');
    expect(cards[0].textContent).toContain('Tiene respuestas: solo se puede desactivar');
    expect(cards[0].querySelector('[role="switch"]')).toBeTruthy();
    expect(cards[0].querySelector('.question-card__edit')).toBeNull();
    expect(cards[2].querySelector('a.question-card__edit')?.getAttribute('href')).toBe('/plantillas/10/preguntas/3');
    expect(el.querySelector('a.template-edit__add')?.getAttribute('href')).toBe('/plantillas/10/preguntas/nueva');
  });

  it('deactivates a locked question right away', () => {
    const { fixture, el } = setup('10');
    service.setQuestionActive.mockReturnValue(of({
      ...detail, questions: detail.questions.map((q) => (q.id === 1 ? { ...q, active: false } : q)),
    }));
    el.querySelector<HTMLButtonElement>('.question-card [role="switch"]')!.click();
    fixture.detectChanges();

    expect(service.setQuestionActive).toHaveBeenCalledWith(10, 1, false);
    expect(el.querySelector('.question-card')?.textContent).toContain('Inactiva');
  });

  it('saves the name and the active switch, then goes back to the list', () => {
    const { el, navigate, submit } = setup('10');
    el.querySelector<HTMLButtonElement>('.template-edit__active [role="switch"]')!.click();
    submit();

    expect(service.update).toHaveBeenCalledWith(10, { name: 'Fractura de muñeca', segmentId: 1 });
    expect(service.setActive).toHaveBeenCalledWith(10, false);
    expect(navigate).toHaveBeenCalledWith(['/plantillas'], { replaceUrl: false });
  });
});
