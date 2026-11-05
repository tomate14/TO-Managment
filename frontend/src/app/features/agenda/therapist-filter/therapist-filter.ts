import { Component, input, output } from '@angular/core';
import { AgendaTherapist } from '../agenda.models';

/** Admin-only filter: one checkbox per therapist with the number of appointments in the visible week. */
@Component({
  selector: 'app-therapist-filter',
  templateUrl: './therapist-filter.html',
  styleUrl: './therapist-filter.scss',
})
export class TherapistFilter {
  readonly therapists = input.required<AgendaTherapist[]>();
  readonly selected = input.required<Set<number>>();
  readonly counts = input.required<Map<number, number>>();
  readonly toggle = output<number>();
}
