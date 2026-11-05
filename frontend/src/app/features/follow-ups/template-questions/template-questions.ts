import { Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormRecord, ReactiveFormsModule } from '@angular/forms';
import { NgTemplateOutlet } from '@angular/common';
import { TemplateDetail, TemplateQuestion } from '../../templates/templates.models';
import { rangeHint } from '../follow-up-form';
import { RawAnswers, conditionReason, visibleQuestionIds } from '../question-visibility';

interface QuestionNode {
  question: TemplateQuestion;
  children: QuestionNode[];
}

/**
 * H6.3 template questions of one segment evaluation. Children appear right under their parent, inside a box that
 * says why they are shown, as soon as the parent answer satisfies their condition.
 */
@Component({
  selector: 'app-template-questions',
  imports: [ReactiveFormsModule, NgTemplateOutlet],
  templateUrl: './template-questions.html',
  styleUrl: './template-questions.scss',
})
export class TemplateQuestions implements OnInit {
  private readonly destroyRef = inject(DestroyRef);

  readonly template = input.required<TemplateDetail>();
  readonly answers = input.required<FormRecord<FormControl<string>>>();
  /** Problems by question id. */
  readonly problems = input<Record<number, string>>({});
  readonly showProblems = input(false);
  /** Unique prefix for element ids. */
  readonly idPrefix = input.required<string>();

  private readonly values = signal<RawAnswers>({});

  protected readonly roots = computed(() => {
    const nodes = new Map<number, QuestionNode>();
    this.template().questions.forEach((question) => nodes.set(question.id, { question, children: [] }));
    const roots: QuestionNode[] = [];
    nodes.forEach((node) => {
      const parent = node.question.parentQuestionId === null ? undefined : nodes.get(node.question.parentQuestionId);
      (parent ? parent.children : roots).push(node);
    });
    return roots;
  });
  protected readonly visible = computed(() => visibleQuestionIds(this.template().questions, this.values()));
  protected readonly yesNoOptions = [
    { value: 'true', label: 'Sí' },
    { value: 'false', label: 'No' },
  ] as const;

  ngOnInit(): void {
    this.values.set(this.answers().getRawValue());
    this.answers().valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.values.set(this.answers().getRawValue()));
  }

  protected control(question: TemplateQuestion): FormControl<string> {
    return this.answers().controls[String(question.id)];
  }

  protected value(question: TemplateQuestion): string {
    return this.values()[String(question.id)] ?? '';
  }

  /** Option chips toggle: tapping the chosen one clears it (no question is required). */
  protected choose(question: TemplateQuestion, value: string, toggle: boolean): void {
    const control = this.control(question);
    control.setValue(toggle && control.value === value ? '' : value);
  }

  protected reason(node: QuestionNode, parent: TemplateQuestion): string {
    return node.question.condition ? conditionReason(node.question.condition, parent) : '';
  }

  protected hint(question: TemplateQuestion): string {
    return rangeHint(question);
  }

  protected inputId(question: TemplateQuestion): string {
    return `${this.idPrefix()}-q${question.id}`;
  }
}
