import { Component, computed, input, output, signal } from '@angular/core';
import { AppointmentCard } from '../appointment-card/appointment-card';
import { dayHeading, groupByDay } from '../agenda-dates';
import { Appointment } from '../agenda.models';

export type AppointmentsRange = 'hoy' | 'semana';

/** "Turnos asignados" tab: Hoy / Esta semana filter and cards grouped by day. */
@Component({
  selector: 'app-appointments-list',
  imports: [AppointmentCard],
  templateUrl: './appointments-list.html',
  styleUrl: './appointments-list.scss',
})
export class AppointmentsList {
  readonly appointments = input.required<Appointment[]>();
  readonly today = input.required<string>();
  readonly reschedule = output<Appointment>();
  readonly cancel = output<Appointment>();

  readonly range = signal<AppointmentsRange>('semana');

  protected readonly groups = computed(() => {
    const visible =
      this.range() === 'hoy' ? this.appointments().filter((appointment) => appointment.date === this.today()) : this.appointments();
    return groupByDay(visible).map((group) => ({
      date: group.date,
      appointments: group.appointments,
      heading: `${dayHeading(group.date, this.today())} · ${group.appointments.length} ${group.appointments.length === 1 ? 'turno' : 'turnos'}`,
    }));
  });
}
