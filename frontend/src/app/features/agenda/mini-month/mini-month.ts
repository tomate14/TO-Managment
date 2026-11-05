import { Component, computed, input, output } from '@angular/core';
import { monthGrid, monthTitle } from '../agenda-dates';

const WEEKDAY_LETTERS = ['L', 'M', 'M', 'J', 'V', 'S', 'D'];

interface MonthCell {
  date: string;
  dayNumber: number;
  outside: boolean;
  inWeek: boolean;
  isToday: boolean;
}

/** Desktop mini month: highlights the visible week; picking a day moves the calendar there. */
@Component({
  selector: 'app-mini-month',
  templateUrl: './mini-month.html',
  styleUrl: './mini-month.scss',
})
export class MiniMonth {
  /** Any date inside the displayed month. */
  readonly month = input.required<string>();
  readonly selectedWeek = input.required<string[]>();
  readonly today = input.required<string>();
  readonly pick = output<string>();
  readonly shift = output<-1 | 1>();

  protected readonly letters = WEEKDAY_LETTERS;
  protected readonly title = computed(() => monthTitle(this.month()));
  protected readonly cells = computed<MonthCell[]>(() => {
    const prefix = this.month().slice(0, 7);
    return monthGrid(this.month()).map((date) => ({
      date,
      dayNumber: Number(date.slice(8, 10)),
      outside: !date.startsWith(prefix),
      inWeek: this.selectedWeek().includes(date),
      isToday: date === this.today(),
    }));
  });
}
