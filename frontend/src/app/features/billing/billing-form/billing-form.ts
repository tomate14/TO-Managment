import { Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { apiErrorCode, apiErrorMessage } from '../../../core/api/api-error';
import { formatDate, todayIso } from '../../patients/patient-format';
import { amountFitsLimit, parseAmount } from '../billing-format';
import { BILLING_ERROR_CODES, BilledSessionRef, BillingSummary, CreateBillingPayload } from '../billing.models';
import { BillingService } from '../billing.service';

interface Problems {
  sessions?: string;
  billedOn?: string;
  amount?: string;
}

const SESSIONS_REQUIRED = 'Elegí al menos una sesión.';
const DATE_REQUIRED = 'Ingresá la fecha de facturación.';
const DATE_IN_FUTURE = 'La fecha no puede ser futura.';
const AMOUNT_REQUIRED = 'Ingresá el monto.';
const AMOUNT_NOT_POSITIVE = 'El monto tiene que ser mayor a cero.';
const AMOUNT_INVALID = 'El monto no es válido.';
const SESSION_INVALID = 'Hay sesiones que no son de este diagnóstico o ya están facturadas.';
const SAVE_ERROR = 'No pudimos registrar la facturación. Intentá de nuevo.';

/** Backend field names of a 400 VALIDATION_ERROR, mapped to the form field that shows them. */
const SERVER_FIELDS = new Map<string, keyof Problems>([['amount', 'amount'], ['billedOn', 'billedOn'], ['followUpIds', 'sessions']]);

/**
 * H12.2 billing form shared by the mobile page and the desktop side panel:
 * pending sessions as checkboxes (all checked at start), billing date and amount.
 */
@Component({
  selector: 'app-billing-form',
  imports: [ReactiveFormsModule],
  templateUrl: './billing-form.html',
  styleUrl: './billing-form.scss',
})
export class BillingForm implements OnInit {
  private readonly billingService = inject(BillingService);
  private readonly formBuilder = inject(NonNullableFormBuilder);

  readonly diagnosisId = input.required<number>();
  /** Sessions that can be billed. Read once on init: every one starts checked. */
  readonly pending = input.required<BilledSessionRef[]>();

  /** Message to show on top when the form is re-created after a stale list (see `stale`). */
  readonly notice = input<string | null>(null);

  readonly saved = output<BillingSummary>();
  /** The pending list was out of date (a session was billed meanwhile): the parent should reload it. */
  readonly stale = output<void>();
  readonly cancelled = output<void>();

  protected readonly today = todayIso();
  protected readonly formatDate = formatDate;

  protected readonly form = this.formBuilder.group({
    billedOn: [this.today],
    amount: [''],
  });

  protected readonly selected = signal<ReadonlySet<number>>(new Set());
  protected readonly submitted = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly serverErrors = signal<Problems>({});

  private readonly value = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });

  protected readonly selectedLabel = computed(() => `${this.selected().size} de ${this.pending().length} elegidas`);
  protected readonly problems = computed(() => this.validate());

  constructor() {
    this.form.controls.billedOn.valueChanges.subscribe(() => this.clearServerError('billedOn'));
    this.form.controls.amount.valueChanges.subscribe(() => this.clearServerError('amount'));
  }

  ngOnInit(): void {
    this.selected.set(new Set(this.pending().map((session) => session.followUpId)));
    this.error.set(this.notice());
  }

  protected isSelected(session: BilledSessionRef): boolean {
    return this.selected().has(session.followUpId);
  }

  protected toggle(session: BilledSessionRef): void {
    this.selected.update((ids) => {
      const next = new Set(ids);
      if (next.has(session.followUpId)) {
        next.delete(session.followUpId);
      } else {
        next.add(session.followUpId);
      }
      return next;
    });
    this.clearServerError('sessions');
  }

  /** Message under a field: the backend's, or the client validation once a save was attempted. */
  protected fieldError(field: keyof Problems): string | null {
    return this.serverErrors()[field] ?? (this.submitted() ? (this.problems()[field] ?? null) : null);
  }

  protected submit(): void {
    if (this.saving()) {
      return;
    }
    this.submitted.set(true);
    this.error.set(null);
    this.serverErrors.set({});
    if (Object.keys(this.problems()).length > 0) {
      return;
    }
    this.saving.set(true);
    this.billingService.register(this.diagnosisId(), this.payload()).subscribe({
      next: (summary) => {
        this.saving.set(false);
        this.saved.emit(summary);
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.showServerError(err);
      },
    });
  }

  private showServerError(err: unknown): void {
    const message = apiErrorMessage(err);
    switch (apiErrorCode(err)) {
      case BILLING_ERROR_CODES.sessionsRequired:
        this.serverErrors.set({ sessions: message ?? SESSIONS_REQUIRED });
        return;
      case BILLING_ERROR_CODES.sessionInvalid:
        this.error.set(message ?? SESSION_INVALID);
        this.stale.emit();
        return;
      case BILLING_ERROR_CODES.amountInvalid:
        this.serverErrors.set({ amount: message ?? AMOUNT_NOT_POSITIVE });
        return;
      case BILLING_ERROR_CODES.dateInFuture:
        this.serverErrors.set({ billedOn: message ?? DATE_IN_FUTURE });
        return;
      default: {
        const fields = serverFieldErrors(err);
        if (Object.keys(fields).length > 0) {
          this.serverErrors.set(fields);
        } else {
          this.error.set(message ?? SAVE_ERROR);
        }
      }
    }
  }

  private clearServerError(field: keyof Problems): void {
    if (this.serverErrors()[field] !== undefined) {
      this.serverErrors.update((errors) => ({ ...errors, [field]: undefined }));
    }
  }

  private validate(): Problems {
    const problems: Problems = {};
    if (this.selected().size === 0) {
      problems.sessions = SESSIONS_REQUIRED;
    }
    const billedOn = this.value().billedOn ?? '';
    if (billedOn === '') {
      problems.billedOn = DATE_REQUIRED;
    } else if (billedOn > this.today) {
      problems.billedOn = DATE_IN_FUTURE;
    }
    const rawAmount = this.value().amount ?? '';
    const amount = parseAmount(rawAmount);
    if (rawAmount.trim() === '') {
      problems.amount = AMOUNT_REQUIRED;
    } else if (amount === null || amount <= 0) {
      problems.amount = AMOUNT_NOT_POSITIVE;
    } else if (!amountFitsLimit(rawAmount)) {
      problems.amount = AMOUNT_INVALID;
    }
    return problems;
  }

  /** Selected sessions in the order they are listed (oldest first). */
  private payload(): CreateBillingPayload {
    const value = this.form.getRawValue();
    return {
      billedOn: value.billedOn,
      amount: parseAmount(value.amount) ?? 0,
      followUpIds: this.pending().map((session) => session.followUpId).filter((id) => this.selected().has(id)),
    };
  }
}

/** Per-field messages of a 400 VALIDATION_ERROR (`amount`, `billedOn`, `followUpIds[0]`). */
function serverFieldErrors(error: unknown): Problems {
  const problems: Problems = {};
  const fieldErrors: unknown = error instanceof HttpErrorResponse ? error.error?.fieldErrors : null;
  if (!Array.isArray(fieldErrors)) {
    return problems;
  }
  for (const item of fieldErrors) {
    if (typeof item?.field !== 'string' || typeof item.message !== 'string') {
      continue;
    }
    const field = SERVER_FIELDS.get(item.field.replace(/\[\d+\]$/, ''));
    if (field !== undefined && problems[field] === undefined) {
      problems[field] = item.message;
    }
  }
  return problems;
}
