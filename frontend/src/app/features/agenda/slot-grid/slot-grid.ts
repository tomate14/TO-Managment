import { Component, input, output } from '@angular/core';
import { Slot } from '../agenda.models';

/** Grid of start-time buttons; unavailable slots are disabled. Times are shown by their first 5 characters. */
@Component({
  selector: 'app-slot-grid',
  templateUrl: './slot-grid.html',
  styleUrl: './slot-grid.scss',
})
export class SlotGrid {
  readonly slots = input.required<Slot[]>();
  readonly selected = input<string | null>(null);
  readonly pick = output<string>();

  protected time(slot: Slot): string {
    return slot.startTime.slice(0, 5);
  }
}
