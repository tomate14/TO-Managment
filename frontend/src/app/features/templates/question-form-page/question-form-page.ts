import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormArray, FormControl, NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../../core/api/api-error';
import { answerTypeLabel, conditionPreview, displayExpected, operatorSupports, parseDecimal } from '../question-text';
import {
  ANSWER_TYPES, AnswerType, Condition, ConditionOperator, NO_VALUE, OPERATORS, QuestionPayload, TemplateDetail,
  TemplateQuestion, YES_VALUE,
} from '../templates.models';
import { TemplatesService } from '../templates.service';

interface Problems {
  text?: string;
  range?: string;
  options?: string;
  parent?: string;
  operator?: string;
  expected?: string;
}

interface ValueChoice {
  value: string;
  label: string;
}

const MIN_OPTIONS = 2;
const LOAD_ERROR = 'No pudimos cargar la plantilla.';
const SAVE_ERROR = 'No pudimos guardar la pregunta. Intentá de nuevo.';
const OPERATOR_INCOMPATIBLE =
  '«Mayor que» y «Menor que» solo sirven si la pregunta padre es numérica. Elegí «Igual a» o «Distinto de».';
const YES_NO_CHOICES: readonly ValueChoice[] = [
  { value: YES_VALUE, label: 'Sí' },
  { value: NO_VALUE, label: 'No' },
];

/** H3.2/H3.3 new question (`…/preguntas/nueva`) and edit question (`…/preguntas/:questionId`). */
@Component({
  selector: 'app-question-form-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './question-form-page.html',
  styleUrl: './question-form-page.scss',
})
export class QuestionFormPage implements OnInit {
  private readonly templatesService = inject(TemplatesService);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly params = inject(ActivatedRoute).snapshot.paramMap;

  protected readonly templateId = Number(this.params.get('id'));
  protected readonly questionId = this.params.get('questionId') === null ? null : Number(this.params.get('questionId'));
  protected readonly isEdit = this.questionId !== null;
  protected readonly answerTypes = ANSWER_TYPES;
  protected readonly operators = OPERATORS;

  protected readonly form = this.formBuilder.group({
    text: [''],
    answerType: ['YES_NO' as AnswerType],
    minValue: [''],
    maxValue: [''],
    options: this.formBuilder.array([this.formBuilder.control(''), this.formBuilder.control('')]),
    conditional: [false],
    parentId: [null as number | null],
    operator: ['EQUALS' as ConditionOperator],
    expectedValue: [''],
  });

  protected readonly template = signal<TemplateDetail | null>(null);
  protected readonly submitted = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  private readonly value = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });

  protected readonly current = computed(() =>
    this.template()?.questions.find((question) => question.id === this.questionId) ?? null);
  protected readonly locked = computed(() => this.current()?.locked ?? false);
  protected readonly hasDependents = computed(() => this.current()?.hasDependents ?? false);
  protected readonly answerType = computed(() => this.value().answerType ?? 'YES_NO');
  protected readonly conditional = computed(() => this.value().conditional ?? false);

  /** Active questions created before this one (the backend rule that also prevents cycles). */
  protected readonly parentCandidates = computed(() => {
    const self = this.current();
    return (this.template()?.questions ?? []).filter((question) =>
      question.active && question.id !== this.questionId && (self === null || question.displayOrder < self.displayOrder));
  });
  protected readonly parent = computed(() =>
    this.parentCandidates().find((question) => question.id === this.value().parentId) ?? null);
  protected readonly valueChoices = computed<readonly ValueChoice[] | null>(() => {
    const parent = this.parent();
    if (parent?.answerType === 'YES_NO') {
      return YES_NO_CHOICES;
    }
    return parent?.answerType === 'OPTION' ? parent.options.map((option) => ({ value: option, label: option })) : null;
  });
  protected readonly operatorError = computed(() => {
    const parent = this.parent();
    const operator = this.value().operator ?? 'EQUALS';
    return this.conditional() && parent !== null && !operatorSupports(operator, parent.answerType) ? OPERATOR_INCOMPATIBLE : null;
  });
  protected readonly preview = computed(() => {
    const parent = this.parent();
    const expected = (this.value().expectedValue ?? '').trim();
    if (!this.conditional() || parent === null || expected === '' || this.operatorError()) {
      return null;
    }
    return conditionPreview(parent.text, this.value().operator ?? 'EQUALS', displayExpected(expected, parent.answerType));
  });
  protected readonly problems = computed(() => this.validate());

  constructor() {
    this.form.controls.parentId.valueChanges.subscribe(() => this.resetExpectedValue());
  }

  ngOnInit(): void {
    this.templatesService.get(this.templateId).subscribe({
      next: (template) => {
        this.template.set(template);
        const question = template.questions.find((candidate) => candidate.id === this.questionId);
        if (question) {
          this.load(question);
        }
      },
      error: () => this.error.set(LOAD_ERROR),
    });
  }

  protected get options(): FormArray<FormControl<string>> {
    return this.form.controls.options;
  }

  protected typeLabel(type: AnswerType): string {
    return answerTypeLabel(type);
  }

  protected selectType(type: AnswerType): void {
    if (!this.hasDependents()) {
      this.form.controls.answerType.setValue(type);
    }
  }

  protected toggleConditional(): void {
    this.form.controls.conditional.setValue(!this.conditional());
  }

  protected addOption(): void {
    this.options.push(this.formBuilder.control(''));
  }

  protected removeOption(index: number): void {
    if (this.options.length > MIN_OPTIONS) {
      this.options.removeAt(index);
    }
  }

  protected canRemoveOption(): boolean {
    return this.options.length > MIN_OPTIONS && !this.hasDependents();
  }

  protected canDelete(): boolean {
    return this.isEdit && !this.locked() && !this.hasDependents();
  }

  protected submit(): void {
    if (this.saving() || this.locked() || this.template() === null) {
      return;
    }
    this.submitted.set(true);
    this.error.set(null);
    if (Object.keys(this.problems()).length > 0) {
      return;
    }
    this.saving.set(true);
    const payload = this.payload();
    const request = this.questionId === null
      ? this.templatesService.addQuestion(this.templateId, payload)
      : this.templatesService.updateQuestion(this.templateId, this.questionId, payload);
    request.subscribe({
      next: () => {
        this.saving.set(false);
        void this.router.navigate(['/plantillas', this.templateId]);
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.error.set(apiErrorMessage(err) ?? SAVE_ERROR);
      },
    });
  }

  protected deleteQuestion(): void {
    if (this.questionId === null || !this.canDelete() || !window.confirm('¿Eliminar esta pregunta?')) {
      return;
    }
    this.templatesService.deleteQuestion(this.templateId, this.questionId).subscribe({
      next: () => void this.router.navigate(['/plantillas', this.templateId]),
      error: (err: unknown) => this.error.set(apiErrorMessage(err) ?? SAVE_ERROR),
    });
  }

  private load(question: TemplateQuestion): void {
    this.options.clear();
    const labels = question.options.length >= MIN_OPTIONS ? question.options : ['', ''];
    labels.forEach((label) => this.options.push(this.formBuilder.control(label)));
    this.form.patchValue({
      text: question.text,
      answerType: question.answerType,
      minValue: question.minValue === null ? '' : String(question.minValue),
      maxValue: question.maxValue === null ? '' : String(question.maxValue),
      conditional: question.parentQuestionId !== null,
      parentId: question.parentQuestionId,
    });
    if (question.condition) {
      this.form.patchValue({ operator: question.condition.operator, expectedValue: question.condition.expectedValue });
    }
  }

  private resetExpectedValue(): void {
    const choices = this.valueChoicesFor(this.form.controls.parentId.value);
    this.form.controls.expectedValue.setValue(choices?.[0]?.value ?? '');
  }

  private valueChoicesFor(parentId: number | null): readonly ValueChoice[] | null {
    const parent = this.template()?.questions.find((question) => question.id === parentId);
    if (parent?.answerType === 'YES_NO') {
      return YES_NO_CHOICES;
    }
    return parent?.answerType === 'OPTION' ? parent.options.map((option) => ({ value: option, label: option })) : null;
  }

  private validate(): Problems {
    const value = this.value();
    const problems: Problems = {};
    if ((value.text ?? '').trim() === '') {
      problems.text = 'Ingresá el texto de la pregunta.';
    }
    if (this.answerType() === 'NUMERIC') {
      const min = parseDecimal(value.minValue ?? '');
      const max = parseDecimal(value.maxValue ?? '');
      if (Number.isNaN(min) || Number.isNaN(max)) {
        problems.range = 'Ingresá números válidos en mínimo y máximo.';
      } else if (min !== null && max !== null && min > max) {
        problems.range = 'El mínimo no puede ser mayor que el máximo.';
      }
    }
    if (this.answerType() === 'OPTION') {
      const labels = this.optionLabels();
      if (labels.length < MIN_OPTIONS) {
        problems.options = 'Agregá al menos 2 opciones.';
      } else if (new Set(labels.map((label) => label.toLowerCase())).size !== labels.length) {
        problems.options = 'Las opciones no se pueden repetir.';
      }
    }
    if (this.conditional()) {
      const parent = this.parent();
      const expected = (value.expectedValue ?? '').trim();
      if (parent === null) {
        problems.parent = 'Elegí la pregunta padre.';
      } else if (this.operatorError()) {
        problems.operator = this.operatorError()!;
      } else if (expected === '') {
        problems.expected = 'Ingresá el valor esperado.';
      } else if (parent.answerType === 'NUMERIC' && Number.isNaN(parseDecimal(expected))) {
        problems.expected = 'El valor esperado tiene que ser un número.';
      }
    }
    return problems;
  }

  private optionLabels(): string[] {
    return this.options.getRawValue().map((label) => label.trim()).filter((label) => label !== '');
  }

  private payload(): QuestionPayload {
    const value = this.form.getRawValue();
    const type = value.answerType;
    const parent = this.conditional() ? this.parent() : null;
    const expected = value.expectedValue.trim();
    const condition: Condition | null = parent === null ? null : {
      operator: value.operator,
      expectedValue: parent.answerType === 'NUMERIC' ? String(parseDecimal(expected)) : expected,
    };
    return {
      text: value.text.trim(),
      answerType: type,
      minValue: type === 'NUMERIC' ? parseDecimal(value.minValue) : null,
      maxValue: type === 'NUMERIC' ? parseDecimal(value.maxValue) : null,
      options: type === 'OPTION' ? this.optionLabels() : [],
      parentQuestionId: parent?.id ?? null,
      condition,
    };
  }
}
