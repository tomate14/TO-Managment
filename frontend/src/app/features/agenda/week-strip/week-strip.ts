import { Component, computed, input, output } from '@angular/core';
import { monthTitle } from '../agenda-dates';
import { Appointment } from '../agenda.models';

const MAX_DOTS = 3;
const WEEKDAY_LETTERS = ['D', 'L', 'M', 'M', 'J', 'V', 'S'];

interface StripDay {
  date: string;
  letter: string;
  dayNumber: number;
  selected: boolean;
  isToday: boolean;
  colors: number[];
}

/** Mobile week strip: 7 days with up to 3 dots (distinct therapist colors of that day). */
@Component({
  selector: 'app-week-strip',
  templateUrl: './week-strip.html',
  styleUrl: './week-strip.scss',
})
export class WeekStrip {
  readonly days = input.required<string[]>();
  readonly selected = input.required<string>();
  readonly today = input.required<string>();
  readonly appointments = input.required<Appointment[]>();
  readonly pick = output<string>();
  readonly shift = output<-1 | 1>();

  protected readonly title = computed(() => monthTitle(this.selected()));
  protected readonly items = computed<StripDay[]>(() =>
    this.days().map((date) => {
      const colors = new Set(
        this.appointments()
          .filter((appointment) => appointment.date === date)
          .map((appointment) => appointment.therapist.color),
      );
      return {
        date,
        letter: WEEKDAY_LETTERS[new Date(`${date}T00:00:00Z`).getUTCDay()],
        dayNumber: Number(date.slice(8, 10)),
        selected: date === this.selected(),
        isToday: date === this.today(),
        colors: [...colors].sort().slice(0, MAX_DOTS),
      };
    }),
  );
}
