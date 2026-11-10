import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Observable, finalize, forkJoin, of } from 'rxjs';
import { apiErrorMessage } from '../../../core/api/api-error';
import { AuthService } from '../../../core/auth/auth.service';
import { AGENDA_TODAY } from '../agenda-dates';
import { AgendaTherapist, Appointment } from '../agenda.models';
import { AgendaService } from '../agenda.service';
import { AppointmentForm } from '../appointment-form/appointment-form';
import { AppointmentCard } from '../appointment-card/appointment-card';

const LOAD_ERROR = 'No pudimos cargar los datos del turno. Intentá de nuevo en unos minutos.';

/** Mobile screens `/agenda/nuevo` and `/agenda/:id/reprogramar`: the shared form full-page. */
@Component({
  selector: 'app-appointment-form-page',
  imports: [AppointmentForm, AppointmentCard, RouterLink],
  templateUrl: './appointment-form-page.html',
  styleUrl: './appointment-form-page.scss',
})
export class AppointmentFormPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly agendaService = inject(AgendaService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly currentUser = inject(AuthService).currentUser;
  protected readonly today = inject(AGENDA_TODAY)();
  private readonly id = this.route.snapshot.paramMap.get('id');
  protected readonly mode = this.id === null ? 'new' : 'reschedule';
  protected readonly details = signal(this.route.snapshot.queryParamMap.get('detalle') === '1');
  protected readonly title = computed(() => this.mode === 'new' ? 'Nuevo turno'
    : this.details() || this.appointment()?.confirmedAt ? 'Detalle del turno' : 'Reprogramar turno');
  private readonly vista = this.route.snapshot.queryParamMap.get('vista');
  /** Keeps the agenda tab the user came from on the header back link. */
  protected readonly backQueryParams = this.vista === null ? {} : { vista: this.vista };
  protected readonly therapists = signal<AgendaTherapist[]>([]);
  protected readonly appointment = signal<Appointment | null>(null);
  protected readonly ready = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly actionBusy = signal(false);
  protected readonly actionError = signal<string | null>(null);

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

  protected reschedule(): void {
    if (!this.actionBusy() && !this.appointment()?.confirmedAt) {
      this.details.set(false);
      this.actionError.set(null);
    }
  }

  protected confirmAppointment(): void {
    const appointment = this.appointment();
    if (!appointment || appointment.confirmedAt || this.actionBusy()) {
      return;
    }
    this.runAction(this.agendaService.confirm(appointment.id), 'No pudimos confirmar la sesión. Intentá de nuevo.',
      (confirmed) => this.appointment.set(confirmed));
  }

  protected cancelAppointment(): void {
    const appointment = this.appointment();
    if (!appointment || appointment.confirmedAt || this.actionBusy()) {
      return;
    }
    this.runAction(this.agendaService.cancel(appointment.id), 'No pudimos cancelar el turno. Intentá de nuevo.', () => this.back());
  }

  private runAction<T>(request: Observable<T>, fallback: string, success: (value: T) => void): void {
    this.actionBusy.set(true);
    this.actionError.set(null);
    request.pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.actionBusy.set(false)),
    ).subscribe({
      next: success,
      error: (err: unknown) => this.actionError.set(apiErrorMessage(err) ?? fallback),
    });
  }
}
