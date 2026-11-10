import { Component, computed, input, output, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { fullName } from '../../patients/patient-format';
import { Appointment } from '../agenda.models';
import { longDate } from '../agenda-dates';

/** One assigned appointment; the cancel confirmation is shown inline inside the card. */
@Component({
  selector: 'app-appointment-card',
  imports: [RouterLink],
  templateUrl: './appointment-card.html',
  styleUrl: './appointment-card.scss',
})
export class AppointmentCard {
  readonly appointment = input.required<Appointment>();
  readonly confirmationBusy = input(false);
  readonly confirmationError = input<string | null>(null);
  readonly showDate = input(false);
  readonly reschedule = output<Appointment>();
  readonly cancel = output<Appointment>();
  readonly confirmSession = output<Appointment>();

  protected readonly confirming = signal(false);
  protected readonly dateLabel = computed(() => `${longDate(this.appointment().date)} de ${this.appointment().date.slice(0, 4)}`);
  protected readonly patientName = computed(() => fullName(this.appointment().patient));
  protected readonly start = computed(() => this.appointment().startTime.slice(0, 5));
  protected readonly end = computed(() => this.appointment().endTime.slice(0, 5));
  protected readonly therapistClass = computed(() => `therapist-${this.appointment().therapist.color}`);

  protected confirmCancel(): void {
    this.confirming.set(false);
    this.cancel.emit(this.appointment());
  }

  protected confirmAppointment(): void {
    if (!this.confirmationBusy() && !this.appointment().confirmedAt) {
      this.confirmSession.emit(this.appointment());
    }
  }
}
