import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

/** Placeholder: Task 10 replaces this with the booking / rescheduling form. */
@Component({
  selector: 'app-appointment-form-page',
  imports: [RouterLink],
  template: `
    <header class="page-header">
      <a class="page-header__back" routerLink="/agenda" aria-label="Volver a la agenda">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M15 5l-7 7 7 7"></path></svg>
      </a>
      <h1 class="page-header__title">{{ title }}</h1>
    </header>
  `,
})
export class AppointmentFormPage {
  protected readonly title = inject(ActivatedRoute).snapshot.paramMap.has('id') ? 'Reprogramar turno' : 'Nuevo turno';
}
