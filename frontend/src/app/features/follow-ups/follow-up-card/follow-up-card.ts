import { Component, computed, input, signal } from '@angular/core';
import { formatDate } from '../../patients/patient-format';
import { answerCountLabel, dateChip, formatAnswer, formatNumber } from '../follow-up-format';
import { FollowUp, SegmentEvaluation } from '../follow-ups.models';

/**
 * H6.4 one follow-up in the history: date, scar type, and per segment its metrics and the template answers
 * (folded until opened).
 */
@Component({
  selector: 'app-follow-up-card',
  templateUrl: './follow-up-card.html',
  styleUrl: './follow-up-card.scss',
})
export class FollowUpCard {
  readonly followUp = input.required<FollowUp>();

  protected readonly chip = computed(() => dateChip(this.followUp().date));
  private readonly expanded = signal(new Set<number>());

  protected readonly formatDate = formatDate;
  protected readonly formatAnswer = formatAnswer;
  protected readonly answerCountLabel = answerCountLabel;

  protected metric(value: number | null): string {
    return value === null ? '—' : formatNumber(value);
  }

  protected isExpanded(evaluation: SegmentEvaluation): boolean {
    return this.expanded().has(evaluation.id);
  }

  protected toggle(evaluation: SegmentEvaluation): void {
    const expanded = new Set(this.expanded());
    if (!expanded.delete(evaluation.id)) {
      expanded.add(evaluation.id);
    }
    this.expanded.set(expanded);
  }
}
