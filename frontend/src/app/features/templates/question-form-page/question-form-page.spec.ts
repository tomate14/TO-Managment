import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { QuestionFormPage } from './question-form-page';
import { TemplatesService } from '../templates.service';
import { TemplateDetail, TemplateQuestion } from '../templates.models';

function question(overrides: Partial<TemplateQuestion>): TemplateQuestion {
  return {
    id: 1, number: 1, depth: 0, displayOrder: 1, text: 'Pregunta', answerType: 'TEXT', minValue: null, maxValue: null,
    options: [], parentQuestionId: null, condition: null, active: true, locked: false, hasDependents: false,
    ...overrides,
  };
}

describe('QuestionFormPage', () => {
  const detail: TemplateDetail = {
    id: 10, name: 'Fractura de muñeca', segment: { id: 1, name: 'Muñeca', active: true }, active: true,
    questions: [
      question({ id: 1, displayOrder: 1, text: '¿Usa férula?', answerType: 'YES_NO', hasDependents: true }),
      question({ id: 2, displayOrder: 2, text: 'Tipo de agarre afectado', answerType: 'OPTION', options: ['Cilíndrico', 'Esférico'] }),
      question({ id: 3, number: 3, displayOrder: 3, text: 'Horas de uso por día', answerType: 'NUMERIC', minValue: 0, maxValue: 24,
        parentQuestionId: 1, condition: { operator: 'EQUALS', expectedValue: 'true' } }),
      question({ id: 4, displayOrder: 4, text: 'Dolor', answerType: 'NUMERIC', active: false }),
    ],
  };
  let service: Record<'get' | 'addQuestion' | 'updateQuestion' | 'deleteQuestion', ReturnType<typeof vi.fn>>;

  function setup(questionId: string | null) {
    service = {
      get: vi.fn(() => of(detail)),
      addQuestion: vi.fn(() => of(detail)),
      updateQuestion: vi.fn(() => of(detail)),
      deleteQuestion: vi.fn(() => of(undefined)),
    };
    const params: Record<string, string> = questionId === null ? { id: '10' } : { id: '10', questionId };
    TestBed.configureTestingModule({
      imports: [QuestionFormPage],
      providers: [
        provideRouter([]),
        { provide: TemplatesService, useValue: service },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap(params) } } },
      ],
    });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(QuestionFormPage);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const type = (selector: string, value: string) => {
      const input = el.querySelector<HTMLInputElement>(selector)!;
      input.value = value;
      input.dispatchEvent(new Event('input'));
      fixture.detectChanges();
    };
    const choose = (selector: string, index: number) => {
      const select = el.querySelector<HTMLSelectElement>(selector)!;
      select.value = select.options[index].value;
      select.dispatchEvent(new Event('change'));
      fixture.detectChanges();
    };
    const click = (text: string) => {
      Array.from(el.querySelectorAll<HTMLButtonElement>('button')).find((b) => b.textContent?.trim() === text)!.click();
      fixture.detectChanges();
    };
    const submit = () => {
      el.querySelector('form')!.dispatchEvent(new Event('submit'));
      fixture.detectChanges();
    };
    return { fixture, el, navigate, type, choose, click, submit };
  }

  it('shows the template in the subtitle and the four answer types', () => {
    const { el } = setup(null);
    expect(el.querySelector('h1')?.textContent).toContain('Nueva pregunta');
    expect(el.textContent).toContain('Fractura de muñeca · Muñeca');
    const types = Array.from(el.querySelectorAll('.type-chip')).map((chip) => chip.textContent?.trim());
    expect(types).toEqual(['Numérica', 'Sí / No', 'Opción', 'Texto']);
  });

  it('adds a numeric question with a range', () => {
    const { el, navigate, type, click, submit } = setup(null);
    type('#question-text', 'Horas de uso por día');
    click('Numérica');
    type('#question-min', '0');
    type('#question-max', '24');
    submit();

    expect(service.addQuestion).toHaveBeenCalledWith(10, {
      text: 'Horas de uso por día', answerType: 'NUMERIC', minValue: 0, maxValue: 24, options: [],
      parentQuestionId: null, condition: null,
    });
    expect(navigate).toHaveBeenCalledWith(['/plantillas', 10]);
    expect(el.querySelector('.field__error')).toBeNull();
  });

  it('rejects a range with min above max', () => {
    const { el, type, click, submit } = setup(null);
    type('#question-text', 'Horas');
    click('Numérica');
    type('#question-min', '25');
    type('#question-max', '24');
    submit();
    expect(service.addQuestion).not.toHaveBeenCalled();
    expect(el.textContent).toContain('El mínimo no puede ser mayor que el máximo.');
  });

  it('needs at least two options and can add more', () => {
    const { el, type, click, submit } = setup(null);
    type('#question-text', 'Tipo de agarre');
    click('Opción');
    expect(el.querySelectorAll('[aria-label^="Opción "]').length).toBe(2);
    type('[aria-label="Opción 1"]', 'Cilíndrico');
    submit();
    expect(el.textContent).toContain('Agregá al menos 2 opciones.');

    click('+ Agregar opción');
    expect(el.querySelectorAll('[aria-label^="Opción "]').length).toBe(3);
    type('[aria-label="Opción 2"]', 'Esférico');
    submit();
    expect(service.addQuestion).toHaveBeenCalledWith(10, expect.objectContaining({
      answerType: 'OPTION', options: ['Cilíndrico', 'Esférico'],
    }));
  });

  it('chains to an earlier active question with a yes/no value and a preview', () => {
    const { el, type, choose, submit } = setup(null);
    type('#question-text', 'Horas de uso por día');
    el.querySelector<HTMLButtonElement>('[aria-labelledby="condition-label"]')!.click();
    const parents = Array.from(el.querySelectorAll<HTMLOptionElement>('#question-parent option')).map((o) => o.textContent?.trim());
    expect(parents).toContain('¿Usa férula? (Sí / No)');
    expect(parents).not.toContain('Dolor (Numérica)');

    choose('#question-parent', 1);
    expect(el.querySelector('.question-form__preview')?.textContent).toContain('aparece cuando «¿Usa férula?» es igual a «Sí»');
    submit();

    expect(service.addQuestion).toHaveBeenCalledWith(10, expect.objectContaining({
      parentQuestionId: 1, condition: { operator: 'EQUALS', expectedValue: 'true' },
    }));
  });

  it('shows the mockup error for greater-than on a yes/no parent and does not save', () => {
    const { el, type, choose, submit } = setup(null);
    type('#question-text', 'Horas');
    el.querySelector<HTMLButtonElement>('[aria-labelledby="condition-label"]')!.click();
    choose('#question-parent', 1);
    choose('#question-operator', 2);

    expect(el.querySelector('#question-operator-error')?.textContent).toContain('«Mayor que» y «Menor que» solo sirven si la pregunta padre es numérica');
    submit();
    expect(service.addQuestion).not.toHaveBeenCalled();
  });

  it('offers the parent options as expected values', () => {
    const { el, type, choose } = setup(null);
    type('#question-text', 'Detalle');
    el.querySelector<HTMLButtonElement>('[aria-labelledby="condition-label"]')!.click();
    choose('#question-parent', 2);
    const values = Array.from(el.querySelectorAll<HTMLOptionElement>('#question-expected option')).map((o) => o.textContent?.trim());
    expect(values).toEqual(['Cilíndrico', 'Esférico']);
  });

  it('edits a chained question, offering only earlier questions as parents', () => {
    const { el, navigate, type, submit } = setup('3');
    expect(el.querySelector('h1')?.textContent).toContain('Editar pregunta');
    expect(el.querySelector<HTMLInputElement>('#question-text')?.value).toBe('Horas de uso por día');
    expect(el.querySelector<HTMLInputElement>('#question-max')?.value).toBe('24');
    const parents = Array.from(el.querySelectorAll<HTMLOptionElement>('#question-parent option')).map((o) => o.textContent?.trim());
    expect(parents).not.toContain('Horas de uso por día (Numérica)');

    type('#question-text', 'Horas de uso diario');
    submit();
    expect(service.updateQuestion).toHaveBeenCalledWith(10, 3, expect.objectContaining({
      text: 'Horas de uso diario', parentQuestionId: 1, condition: { operator: 'EQUALS', expectedValue: 'true' },
    }));
    expect(navigate).toHaveBeenCalledWith(['/plantillas', 10]);
  });

  it('locks type and options of a question others depend on, and hides delete', () => {
    const { el } = setup('1');
    el.querySelectorAll<HTMLButtonElement>('.type-chip').forEach((chip) => expect(chip.disabled).toBe(true));
    expect(el.textContent).toContain('Otras preguntas dependen de esta');
    expect(el.querySelector('.question-form__delete')).toBeNull();
  });

  it('deletes a question after confirming', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const { navigate, click } = setup('2');
    click('Eliminar pregunta');
    expect(service.deleteQuestion).toHaveBeenCalledWith(10, 2);
    expect(navigate).toHaveBeenCalledWith(['/plantillas', 10]);
  });

  it('shows backend errors in a banner', () => {
    const { el, type, submit } = setup(null);
    service.addQuestion.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 422, error: { code: 'CONDITION_VALUE_INVALID', message: 'El valor esperado no es una respuesta válida de la pregunta padre.' },
    })));
    type('#question-text', 'Algo');
    submit();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('El valor esperado no es una respuesta válida');
  });
});
