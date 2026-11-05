import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { AGENDA_TODAY } from '../agenda-dates';
import { AgendaTherapist, Appointment } from '../agenda.models';
import { AgendaService } from '../agenda.service';
import { AppointmentForm } from '../appointment-form/appointment-form';

const LOAD_ERROR = 'No pudimos cargar los datos del turno. Intentá de nuevo en unos minutos.';

/** Mobile screens `/agenda/nuevo` and `/agenda/:id/reprogramar`: the shared form full-page. */
@Component({
  selector: 'app-appointment-form-page',
  imports: [AppointmentForm, RouterLink],
  templateUrl: './appointment-form-page.html',
  styleUrl: './appointment-form-page.scss',
})
export class AppointmentFormPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly agendaService = inject(AgendaService);

  protected readonly currentUser = inject(AuthService).currentUser;
  protected readonly today = inject(AGENDA_TODAY)();
  private readonly id = this.route.snapshot.paramMap.get('id');
  protected readonly mode = this.id === null ? 'new' : 'reschedule';
  protected readonly title = this.mode === 'new' ? 'Nuevo turno' : 'Reprogramar turno';
  private readonly vista = this.route.snapshot.queryParamMap.get('vista');
  /** Keeps the agenda tab the user came from on the header back link. */
  protected readonly backQueryParams = this.vista === null ? {} : { vista: this.vista };
  protected readonly therapists = signal<AgendaTherapist[]>([]);
  protected readonly appointment = signal<Appointment | null>(null);
  protected readonly ready = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    forkJoin({
      therapists: this.agendaService.therapists(),
      appointment: this.id === null ? of(null) : this.agendaService.get(Number(this.id)),
    }).subscribe({
      next: ({ therapists, appointment }) => {
        this.therapists.set(therapists);
        this.appointment.set(appointment);
        this.ready.set(true);
      },
      error: () => this.error.set(LOAD_ERROR),
    });
  }

  protected back(): void {
    void this.router.navigate(['/agenda'], this.vista === null ? {} : { queryParams: { vista: this.vista } });
  }
}
