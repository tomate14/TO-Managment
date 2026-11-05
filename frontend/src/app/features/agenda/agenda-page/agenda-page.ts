import { Component, DestroyRef, computed, inject, linkedSignal, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { EMPTY, catchError, of, switchMap } from 'rxjs';
import { apiErrorMessage } from '../../../core/api/api-error';
import { AuthService } from '../../../core/auth/auth.service';
import { injectIsDesktop } from '../../../layout/desktop-media';
import { AgendaTabs, AgendaView } from '../agenda-tabs/agenda-tabs';
import { AGENDA_TODAY, addDays, shiftMonth, weekDays, weekStart, weekTitle } from '../agenda-dates';
import { AgendaTherapist, Appointment } from '../agenda.models';
import { AgendaService } from '../agenda.service';
import { AppointmentsList } from '../appointments-list/appointments-list';
import { DayTimeline } from '../day-timeline/day-timeline';
import { MiniMonth } from '../mini-month/mini-month';
import { TherapistFilter } from '../therapist-filter/therapist-filter';
import { WeekCalendar } from '../week-calendar/week-calendar';
import { WeekStrip } from '../week-strip/week-strip';

export type AgendaForm = { mode: 'new' } | { mode: 'reschedule'; appointment: Appointment };

const LOAD_ERROR = 'No pudimos cargar la agenda. Intentá de nuevo en unos minutos.';
const CANCEL_ERROR = 'No pudimos cancelar el turno. Intentá de nuevo.';
const CLOCK_REFRESH_MS = 60_000;
const WEEK_LENGTH = 7;

/** Current local time as "HH:mm". */
function currentTime(): string {
  const date = new Date();
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}

/** E7 agenda screen: header, view tabs and the visible week's appointments (calendar / cards plug in here). */
@Component({
  selector: 'app-agenda-page',
  imports: [AgendaTabs, AppointmentsList, DayTimeline, MiniMonth, TherapistFilter, WeekCalendar, WeekStrip],
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
  protected readonly today = inject(AGENDA_TODAY)();
  protected readonly anchorDate = signal(this.today);
  /** Month shown by the mini month; follows the anchor date until the user pages through months. */
  protected readonly displayMonth = linkedSignal(() => this.anchorDate());
  protected readonly now = signal(currentTime());
  protected readonly days = computed(() => weekDays(this.anchorDate()));
  protected readonly title = computed(() => weekTitle(this.anchorDate()));
  protected readonly counts = computed(() => {
    const counts = new Map<number, number>();
    for (const appointment of this.appointments()) {
      counts.set(appointment.therapist.id, (counts.get(appointment.therapist.id) ?? 0) + 1);
    }
    return counts;
  });
  protected readonly therapists = signal<AgendaTherapist[]>([]);
  protected readonly selectedTherapistIds = signal<Set<number>>(new Set());
  /** Admins only: the week is not requested until the therapist list (and its default selection) has arrived. */
  private readonly therapistsLoaded = signal(false);
  protected readonly appointments = signal<Appointment[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly formOpen = signal<AgendaForm | null>(null);

  protected readonly isAdmin = computed(() => this.auth.currentUser()?.role === 'ADMIN');
  protected readonly subtitle = computed(() => (this.isAdmin() ? 'Turnos de todo el equipo.' : 'Tus turnos.'));

  constructor() {
    const clock = setInterval(() => this.now.set(currentTime()), CLOCK_REFRESH_MS);
    inject(DestroyRef).onDestroy(() => clearInterval(clock));

    toObservable(
      computed(() => ({
        anchor: this.anchorDate(),
        ids: [...this.selectedTherapistIds()],
        tick: this.reloadTick(),
        ready: !this.isAdmin() || this.therapistsLoaded(),
      })),
    )
      .pipe(
        switchMap(({ anchor, ids, ready }) => {
          if (!ready) {
            return EMPTY;
          }
          // An empty list means "no filter" for the service, so an admin with nothing selected sees nothing.
          if (this.isAdmin() && ids.length === 0) {
            return of<Appointment[]>([]);
          }
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
          this.therapistsLoaded.set(true);
        },
        error: () => {
          this.therapists.set([]);
          this.therapistsLoaded.set(true);
        },
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

  protected goToday(): void {
    this.anchorDate.set(this.today);
  }

  protected shiftWeek(direction: -1 | 1): void {
    this.anchorDate.update((anchor) => addDays(anchor, direction * WEEK_LENGTH));
  }

  protected shiftMonth(direction: -1 | 1): void {
    this.displayMonth.update((month) => shiftMonth(month, direction));
  }

  protected openAppointment(appointment: Appointment): void {
    if (this.isDesktop()) {
      this.formOpen.set({ mode: 'reschedule', appointment });
    } else {
      void this.router.navigate(['/agenda', appointment.id, 'reprogramar']);
    }
  }

  protected cancelAppointment(appointment: Appointment): void {
    this.agendaService.cancel(appointment.id).subscribe({
      next: () => {
        this.error.set(null);
        this.reload();
      },
      error: (err: unknown) => this.error.set(apiErrorMessage(err) ?? CANCEL_ERROR),
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
