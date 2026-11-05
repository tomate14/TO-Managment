import { Component, computed, input, output } from '@angular/core';
import { layoutDay, nowOffset, PositionedAppointment } from '../agenda-layout';
import { Appointment } from '../agenda.models';

const PX_PER_HOUR = 52;
const FIRST_HOUR = 8;
const HOUR_COUNT = 12;
const WEEKDAY_LABELS = ['DOM', 'LUN', 'MAR', 'MIÉ', 'JUE', 'VIE', 'SÁB'];

interface DayColumn {
  date: string;
  weekday: string;
  dayNumber: number;
  isToday: boolean;
  blocks: PositionedAppointment[];
  nowTop: number | null;
}

/** Desktop week grid: one column per day, hours 08:00–19:00, blocks colored by therapist. */
@Component({
  selector: 'app-week-calendar',
  templateUrl: './week-calendar.html',
  styleUrl: './week-calendar.scss',
})
export class WeekCalendar {
  readonly days = input.required<string[]>();
  readonly appointments = input.required<Appointment[]>();
  readonly today = input.required<string>();
  /** Current time "HH:mm". */
  readonly now = input.required<string>();
  readonly select = output<Appointment>();

  protected readonly hours = Array.from({ length: HOUR_COUNT }, (_, index) => ({
    top: index * PX_PER_HOUR,
    label: `${String(FIRST_HOUR + index).padStart(2, '0')}:00`,
  }));
  protected readonly gridHeight = HOUR_COUNT * PX_PER_HOUR;

  protected readonly columns = computed<DayColumn[]>(() =>
    this.days().map((date) => {
      const isToday = date === this.today();
      return {
        date,
        weekday: WEEKDAY_LABELS[new Date(`${date}T00:00:00Z`).getUTCDay()],
        dayNumber: Number(date.slice(8, 10)),
        isToday,
        blocks: layoutDay(
          this.appointments().filter((appointment) => appointment.date === date),
          PX_PER_HOUR,
        ),
        nowTop: isToday ? nowOffset(this.now(), PX_PER_HOUR) : null,
      };
    }),
  );

  protected patientName(appointment: Appointment): string {
    return `${appointment.patient.firstName} ${appointment.patient.lastName}`;
  }

  protected range(appointment: Appointment): string {
    return `${appointment.startTime.slice(0, 5)}–${appointment.endTime.slice(0, 5)}`;
  }

  protected label(appointment: Appointment): string {
    return `${this.patientName(appointment)}, ${this.range(appointment)}, ${appointment.therapist.fullName}`;
  }
}
