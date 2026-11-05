import { Component, computed, input, output } from '@angular/core';
import { dayHeading } from '../agenda-dates';
import { layoutDay, nowOffset } from '../agenda-layout';
import { Appointment } from '../agenda.models';

const PX_PER_HOUR = 64;
const FIRST_HOUR = 8;
const HOUR_COUNT = 12;

/** Mobile day view: hour timeline (64 px/h) of one day. */
@Component({
  selector: 'app-day-timeline',
  templateUrl: './day-timeline.html',
  styleUrl: './day-timeline.scss',
})
export class DayTimeline {
  readonly date = input.required<string>();
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

  private readonly dayAppointments = computed(() => this.appointments().filter((a) => a.date === this.date()));
  protected readonly blocks = computed(() => layoutDay(this.dayAppointments(), PX_PER_HOUR));
  protected readonly heading = computed(() => dayHeading(this.date(), this.today()));
  protected readonly countText = computed(() => {
    const count = this.dayAppointments().length;
    return count === 1 ? '1 turno' : `${count} turnos`;
  });
  protected readonly nowTop = computed(() => (this.date() === this.today() ? nowOffset(this.now(), PX_PER_HOUR) : null));

  protected patientName(appointment: Appointment): string {
    return `${appointment.patient.firstName} ${appointment.patient.lastName}`;
  }

  protected detail(appointment: Appointment): string {
    const range = `${appointment.startTime.slice(0, 5)}–${appointment.endTime.slice(0, 5)}`;
    return `${range} · ${appointment.therapist.fullName.split(' ')[0]}`;
  }
}
