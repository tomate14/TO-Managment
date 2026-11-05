import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, of, switchMap } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { injectIsDesktop } from '../../../layout/desktop-media';
import { AgendaTabs, AgendaView } from '../agenda-tabs/agenda-tabs';
import { AGENDA_TODAY, addDays, weekStart } from '../agenda-dates';
import { AgendaTherapist, Appointment } from '../agenda.models';
import { AgendaService } from '../agenda.service';

export type AgendaForm = { mode: 'new' } | { mode: 'reschedule'; appointment: Appointment };

const LOAD_ERROR = 'No pudimos cargar la agenda. Intentá de nuevo en unos minutos.';

/** E7 agenda screen: header, view tabs and the visible week's appointments (calendar / cards plug in here). */
@Component({
  selector: 'app-agenda-page',
  imports: [AgendaTabs],
  templateUrl: './agenda-page.html',
  styleUrl: './agenda-page.scss',
})
export class AgendaPage {
  private readonly agendaService = inject(AgendaService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly isDesktop = injectIsDesktop();
  private readonly reloadTick = signal(0);

  private readonly query = toSignal(this.route.queryParamMap, { initialValue: this.route.snapshot.queryParamMap });

  protected readonly view = computed<AgendaView>(() => (this.query().get('vista') === 'turnos' ? 'turnos' : 'calendario'));
  protected readonly anchorDate = signal(inject(AGENDA_TODAY)());
  protected readonly therapists = signal<AgendaTherapist[]>([]);
  protected readonly selectedTherapistIds = signal<Set<number>>(new Set());
  protected readonly appointments = signal<Appointment[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly formOpen = signal<AgendaForm | null>(null);

  protected readonly isAdmin = computed(() => this.auth.currentUser()?.role === 'ADMIN');
  protected readonly subtitle = computed(() => (this.isAdmin() ? 'Turnos de todo el equipo.' : 'Tus turnos.'));

  constructor() {
    toObservable(
      computed(() => ({
        anchor: this.anchorDate(),
        ids: [...this.selectedTherapistIds()],
        tick: this.reloadTick(),
      })),
    )
      .pipe(
        switchMap(({ anchor, ids }) => {
          const start = weekStart(anchor);
          return this.agendaService.list(start, addDays(start, 6), ids).pipe(
            catchError(() => {
              this.error.set(LOAD_ERROR);
              return of(null);
            }),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((appointments) => {
        if (appointments) {
          this.error.set(null);
          this.appointments.set(appointments);
        }
      });

    if (this.auth.currentUser()?.role === 'ADMIN') {
      this.agendaService.therapists().subscribe({
        next: (therapists) => {
          this.therapists.set(therapists);
          this.selectedTherapistIds.set(new Set(therapists.map((therapist) => therapist.id)));
        },
        error: () => this.therapists.set([]),
      });
    }
  }

  reload(): void {
    this.reloadTick.update((tick) => tick + 1);
  }

  protected changeView(view: AgendaView): void {
    void this.router.navigate([], { relativeTo: this.route, queryParams: { vista: view }, queryParamsHandling: 'merge' });
  }

  protected toggleTherapist(id: number): void {
    this.selectedTherapistIds.update((current) => {
      const next = new Set(current);
      if (!next.delete(id)) {
        next.add(id);
      }
      return next;
    });
  }

  protected newAppointment(): void {
    if (this.isDesktop()) {
      this.formOpen.set({ mode: 'new' });
    } else {
      void this.router.navigate(['/agenda/nuevo']);
    }
  }
}
