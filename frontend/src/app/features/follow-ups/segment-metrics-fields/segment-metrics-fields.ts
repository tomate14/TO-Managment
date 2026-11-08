import { Component, DestroyRef, OnInit, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormRecord, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { SegmentMetric } from '../../segment-metrics/segment-metrics.models';
import { RawAnswers } from '../question-visibility';

/**
 * H11.3 "ROM" block of one segment evaluation: one control per active ROM metric of the segment, by answer type,
 * with the same visual patterns as the template questions.
 */
@Component({
  selector: 'app-segment-metrics-fields',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './segment-metrics-fields.html',
  styleUrl: './segment-metrics-fields.scss',
})
export class SegmentMetricsFields implements OnInit {
  private readonly destroyRef = inject(DestroyRef);

  /** Active metrics only, in their display order. */
  readonly metrics = input.required<SegmentMetric[]>();
  /** One control per metric, by metric id. */
  readonly values = input.required<FormRecord<FormControl<string>>>();
  /** Problems by metric id. */
  readonly problems = input<Record<number, string>>({});
  readonly showProblems = input(false);
  /** Unique prefix for element ids. */
  readonly idPrefix = input.required<string>();
  /** Segment whose metrics are shown, for the "Definir en Catálogos" link. */
  readonly segmentId = input<number | null>(null);
  /** Shows "Definir en Catálogos" when the segment has no metrics (administrators only). */
  readonly canDefine = input(false);

  private readonly current = signal<RawAnswers>({});

  protected readonly yesNoOptions = [
    { value: 'true', label: 'Sí' },
    { value: 'false', label: 'No' },
  ] as const;

  ngOnInit(): void {
    this.current.set(this.values().getRawValue());
    this.values().valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.current.set(this.values().getRawValue()));
  }

  protected control(metric: SegmentMetric): FormControl<string> {
    return this.values().controls[String(metric.id)];
  }

  protected value(metric: SegmentMetric): string {
    return this.current()[String(metric.id)] ?? '';
  }

  /** Option chips toggle: tapping the chosen one clears it. */
  protected choose(metric: SegmentMetric, value: string, toggle: boolean): void {
    const control = this.control(metric);
    control.setValue(toggle && control.value === value ? '' : value);
  }

  protected problem(metric: SegmentMetric): string | undefined {
    return this.showProblems() ? this.problems()[metric.id] : undefined;
  }

  protected inputId(metric: SegmentMetric): string {
    return `${this.idPrefix()}-m${metric.id}`;
  }
}
