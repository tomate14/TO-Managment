import { Component, DestroyRef, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject, catchError, debounceTime, map, of, switchMap, tap } from 'rxjs';
import { apiErrorCode, apiErrorMessage } from '../../../core/api/api-error';
import { UserSummary } from '../../../core/auth/auth.models';
import { Diagnosis } from '../../diagnoses/diagnoses.models';
import { DiagnosesService } from '../../diagnoses/diagnoses.service';
import { PatientForm } from '../../patients/patient-form/patient-form';
import { formatDni, fullName, listName, normalizeDni } from '../../patients/patient-format';
import { Patient } from '../../patients/patients.models';
import { PatientsService } from '../../patients/patients.service';
import { addMinutes, longDate } from '../agenda-dates';
import { AgendaTherapist, Appointment, AppointmentPayload, DURATIONS, Slot } from '../agenda.models';
import { AgendaService } from '../agenda.service';
import { SlotGrid } from '../slot-grid/slot-grid';

const SEARCH_DEBOUNCE_MS = 300;
const SEARCH_SIZE = 8;
const DEFAULT_DURATION = 45;
const NOTES_MAX = 500;
const SAVE_ERROR = 'No pudimos guardar el turno. Intentá de nuevo.';
const SLOTS_ERROR = 'No pudimos cargar los horarios. Intentá de nuevo.';
const BUSY_CODES = ['THERAPIST_BUSY', 'PATIENT_BUSY'];

interface FormValues {
  patientId: number | null;
  diagnosisId: number | null;
  therapistId: number | null;
  date: string;
  durationMinutes: number;
  startTime: string;
  notes: string;
}

/** Shared by the desktop side panel and the mobile pages: books a new appointment or reschedules one. */
@Component({
  selector: 'app-appointment-form',
  imports: [ReactiveFormsModule, SlotGrid, PatientForm],
  templateUrl: './appointment-form.html',
  styleUrl: './appointment-form.scss',
})
export class AppointmentForm implements OnInit {
  private readonly agendaService = inject(AgendaService);
  private readonly patientsService = inject(PatientsService);
  private readonly diagnosesService = inject(DiagnosesService);
  private readonly destroyRef = inject(DestroyRef);

  readonly mode = input.required<'new' | 'reschedule'>();
  readonly appointment = input<Appointment | null>(null);
  readonly therapists = input.required<AgendaTherapist[]>();
  readonly currentUser = input.required<UserSummary>();
  readonly today = input.required<string>();

  readonly saved = output<Appointment>();
  readonly cancelled = output<void>();

  protected readonly formatDni = formatDni;
  protected readonly durations = DURATIONS;
  protected readonly notesMax = NOTES_MAX;

  readonly form = new FormGroup({
    patientId: new FormControl<number | null>(null, Validators.required),
    diagnosisId: new FormControl<number | null>(null),
    therapistId: new FormControl<number | null>(null, Validators.required),
    date: new FormControl('', { nonNullable: true, validators: Validators.required }),
    durationMinutes: new FormControl(DEFAULT_DURATION, { nonNullable: true }),
    startTime: new FormControl('', { nonNullable: true, validators: Validators.required }),
    notes: new FormControl('', { nonNullable: true, validators: Validators.maxLength(NOTES_MAX) }),
  });
  protected readonly searchControl = new FormControl('', { nonNullable: true });

  protected readonly values = signal<FormValues>(this.form.getRawValue());
  protected readonly slots = signal<Slot[]>([]);
  protected readonly diagnoses = signal<Diagnosis[]>([]);
  protected readonly results = signal<Patient[]>([]);
  protected readonly searched = signal(false);
  private readonly searchedText = signal('');
  /** "booking" shows the appointment fields; "register" swaps them for the quick patient registration. */
  protected readonly view = signal<'booking' | 'register'>('booking');
  /** Digits of the last search when it looked like a DNI and found nobody; null otherwise. */
  protected readonly missingDni = computed(() => {
    if (!this.searched() || this.results().length > 0) {
      return null;
    }
    const digits = normalizeDni(this.searchedText());
    return /^\d+$/.test(digits) ? digits : null;
  });
  protected readonly pickedPatient = signal<Pick<Patient, 'id' | 'firstName' | 'lastName' | 'dni'> | null>(null);
  protected readonly slotsError = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  private readonly slotsTick = signal(0);
  private pickedLabel = '';

  protected readonly isTherapist = computed(() => this.currentUser().role === 'TERAPEUTA');
  protected readonly therapistOptions = computed(() => {
    const user = this.currentUser();
    if (user.role === 'TERAPEUTA') {
      return [{ id: user.id, fullName: user.fullName }];
    }
    const currentId = this.appointment()?.therapist.id;
    return this.therapists().filter((therapist) => therapist.active || therapist.id === currentId);
  });
  protected readonly therapistName = computed(
    () => this.therapistOptions().find((therapist) => therapist.id === this.values().therapistId)?.fullName ?? null,
  );
  protected readonly summary = computed(() => {
    const { startTime, date, durationMinutes } = this.values();
    const patient = this.pickedPatient();
    const therapist = this.therapistName();
    if (startTime === '' || date === '' || patient === null || therapist === null) {
      return null;
    }
    return {
      when: `${longDate(date)} · ${startTime} a ${addMinutes(startTime, durationMinutes)}`,
      who: `${fullName(patient)} con ${therapist}`,
    };
  });

  private readonly searchTerms = new Subject<string>();

  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.values.set(this.form.getRawValue()));

    toObservable(computed(() => this.values().patientId))
      .pipe(
        switchMap((patientId) =>
          patientId === null
            ? of<Diagnosis[]>([])
            : this.diagnosesService.listForPatient(patientId).pipe(catchError(() => of<Diagnosis[]>([]))),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((diagnoses) => this.diagnoses.set(diagnoses));

    toObservable(
      computed(() => {
        const { therapistId, patientId, date, durationMinutes } = this.values();
        return { therapistId, patientId, date, durationMinutes, tick: this.slotsTick() };
      }),
    )
      .pipe(
        switchMap(({ therapistId, patientId, date, durationMinutes }) => {
          this.slotsError.set(null);
          if (therapistId === null || date === '') {
            return of<Slot[]>([]);
          }
          const excluded = this.mode() === 'reschedule' ? this.appointment()?.id : undefined;
          return this.agendaService
            .availability({
              therapistId,
              ...(patientId === null ? {} : { patientId }),
              date,
              durationMinutes,
              ...(excluded === undefined ? {} : { excludeAppointmentId: excluded }),
            })
            .pipe(
              catchError(() => {
                this.slotsError.set(SLOTS_ERROR);
                return of(null);
              }),
            );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((slots) => {
        if (slots === null) {
          // Failed request: show no slots but keep the user's chosen time untouched.
          this.slots.set([]);
        } else {
          this.applySlots(slots);
        }
      });

    this.searchControl.valueChanges
      .pipe(
        tap((text) => {
          if (text !== this.pickedLabel) {
            this.pickedLabel = '';
            this.pickedPatient.set(null);
            this.form.controls.patientId.setValue(null);
            this.form.controls.diagnosisId.setValue(null);
          }
        }),
        debounceTime(SEARCH_DEBOUNCE_MS),
        switchMap((text) => {
          if (text.trim() === '' || text === this.pickedLabel) {
            this.searched.set(false);
            return of<Patient[]>([]);
          }
          return this.patientsService.search(text, 0, SEARCH_SIZE).pipe(
            tap(() => {
              this.searchedText.set(text);
              this.searched.set(true);
            }),
            map((page) => page.content),
            catchError(() => of<Patient[]>([])),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((patients) => this.results.set(patients));
  }

  ngOnInit(): void {
    const appointment = this.appointment();
    const user = this.currentUser();
    const isReschedule = this.mode() === 'reschedule' && appointment !== null;
    this.form.patchValue({
      patientId: isReschedule ? appointment.patient.id : null,
      diagnosisId: isReschedule ? (appointment.diagnosis?.id ?? null) : null,
      therapistId: user.role === 'TERAPEUTA' ? user.id : isReschedule ? appointment.therapist.id : this.defaultTherapist(),
      date: isReschedule ? appointment.date : this.today(),
      durationMinutes: isReschedule ? appointment.durationMinutes : DEFAULT_DURATION,
      startTime: isReschedule ? appointment.startTime.slice(0, 5) : '',
      notes: isReschedule ? (appointment.notes ?? '') : '',
    });
    if (isReschedule) {
      this.pickedPatient.set(appointment.patient);
    }
    if (user.role === 'TERAPEUTA') {
      this.form.controls.therapistId.disable();
    }
    this.values.set(this.form.getRawValue());
  }

  protected patientLabel(patient: Pick<Patient, 'firstName' | 'lastName' | 'dni'>): string {
    return `${fullName(patient)} · DNI ${formatDni(patient.dni)}`;
  }

  protected resultLabel(patient: Patient): string {
    return `${listName(patient)} · DNI ${formatDni(patient.dni)}`;
  }

  protected pickPatient(patient: Patient): void {
    this.pickedLabel = this.patientLabel(patient);
    this.searchControl.setValue(this.pickedLabel, { emitEvent: false });
    this.pickedPatient.set(patient);
    this.results.set([]);
    this.searched.set(false);
    this.form.patchValue({ patientId: patient.id, diagnosisId: null });
  }

  protected startRegistration(): void {
    this.view.set('register');
  }

  protected backToBooking(): void {
    this.view.set('booking');
  }

  /** A freshly registered patient is selected exactly as if it had been picked from the search results. */
  protected onPatientRegistered(patient: Patient): void {
    this.pickPatient(patient);
    this.view.set('booking');
  }

  protected pickSlot(time: string): void {
    this.form.controls.startTime.setValue(time);
  }

  protected submit(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const payload: AppointmentPayload = {
      therapistId: value.therapistId!,
      diagnosisId: value.diagnosisId,
      date: value.date,
      startTime: value.startTime,
      durationMinutes: value.durationMinutes,
      notes: value.notes.trim() === '' ? null : value.notes.trim(),
    };
    const appointment = this.appointment();
    const request =
      this.mode() === 'reschedule' && appointment !== null
        ? this.agendaService.reschedule(appointment.id, payload)
        : this.agendaService.book({ ...payload, patientId: value.patientId! });
    this.saving.set(true);
    this.error.set(null);
    request.subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.saved.emit(saved);
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.error.set(apiErrorMessage(err) ?? SAVE_ERROR);
        if (BUSY_CODES.includes(apiErrorCode(err) ?? '')) {
          this.slotsTick.update((tick) => tick + 1);
        }
      },
    });
  }

  private defaultTherapist(): number | null {
    return this.therapistOptions()[0]?.id ?? null;
  }

  /** Stores the slots and clears the chosen start time when it is no longer available. */
  private applySlots(slots: Slot[]): void {
    this.slots.set(slots);
    const chosen = this.form.controls.startTime.value;
    if (chosen !== '' && !slots.some((slot) => slot.available && slot.startTime.slice(0, 5) === chosen)) {
      this.form.controls.startTime.setValue('');
    }
  }
}
