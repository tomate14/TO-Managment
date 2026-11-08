import { Component, ElementRef, OnInit, afterNextRender, computed, effect, input, output, signal, untracked, viewChild } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { todayIso } from '../../patients/patient-format';
import { EstablishmentSummary } from '../establishments.models';
import { trialLabel, trialLastDay } from '../trial-format';

const BLANK_DATE_MESSAGE = 'Elegí una fecha.';

/** H10.3 dialog to set the last trial day or remove the expiry: centered modal on desktop, bottom sheet on mobile. */
@Component({
  selector: 'app-trial-dialog',
  imports: [ReactiveFormsModule],
  templateUrl: './trial-dialog.html',
  styleUrl: './trial-dialog.scss',
})
export class TrialDialog implements OnInit {
  readonly establishment = input.required<EstablishmentSummary>();
  readonly busy = input(false);
  readonly error = input<string | null>(null);
  readonly desktop = input(false);

  readonly save = output<string>();
  readonly remove = output<void>();
  readonly cancel = output<void>();

  protected readonly today = todayIso();
  protected readonly date = new FormControl('', { nonNullable: true });
  protected readonly blankError = signal<string | null>(null);
  /** True once the date changed after a server error: that error is stale until the next one arrives. */
  protected readonly serverErrorDismissed = signal(false);
  protected readonly shownError = computed(() => this.blankError() ?? (this.serverErrorDismissed() ? null : this.error()));
  private readonly dateInput = viewChild<ElementRef<HTMLInputElement>>('dateInput');
  protected readonly subtitle = computed(() => {
    const label = trialLabel(this.establishment());
    return `${this.establishment().name} · ${label.charAt(0).toLowerCase()}${label.slice(1)}`;
  });

  constructor() {
    this.date.valueChanges.subscribe(() => {
      this.blankError.set(null);
      this.serverErrorDismissed.set(true);
    });
    effect(() => {
      this.error();
      untracked(() => this.serverErrorDismissed.set(false));
    });
    afterNextRender(() => this.dateInput()?.nativeElement.focus());
  }

  ngOnInit(): void {
    const endsAt = this.establishment().trialEndsAt;
    if (endsAt !== null) {
      const lastDay = trialLastDay(endsAt);
      if (lastDay >= this.today) {
        this.date.setValue(lastDay);
      }
    }
  }

  protected submit(): void {
    if (this.busy()) {
      return;
    }
    const value = this.date.value;
    if (value === '') {
      this.blankError.set(BLANK_DATE_MESSAGE);
      return;
    }
    this.save.emit(value);
  }
}
