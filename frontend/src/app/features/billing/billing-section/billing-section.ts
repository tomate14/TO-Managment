import { Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { Router } from '@angular/router';
import { apiErrorMessage } from '../../../core/api/api-error';
import { injectIsDesktop } from '../../../layout/desktop-media';
import { formatDate } from '../../patients/patient-format';
import { BillingForm } from '../billing-form/billing-form';
import { formatAmount, formatLongDate, sessionCountLabel } from '../billing-format';
import { Billing, BillingSummary } from '../billing.models';
import { BillingService } from '../billing.service';

const LOAD_ERROR = 'No pudimos cargar la facturación. Intentá de nuevo en unos minutos.';
const STALE_NOTICE = 'Algunas sesiones ya estaban facturadas. Actualizamos la lista: revisá las sesiones y volvé a intentar.';
const VOID_ERROR = 'No pudimos anular la facturación. Intentá de nuevo.';

/**
 * H12.1 / H12.3 "Facturación" section of the patient detail: counters, the register button
 * (side panel on desktop, its own page on mobile) and the billing history with inline voiding.
 */
@Component({
  selector: 'app-billing-section',
  imports: [BillingForm],
  templateUrl: './billing-section.html',
  styleUrl: './billing-section.scss',
})
export class BillingSection implements OnInit {
  private readonly billingService = inject(BillingService);
  private readonly router = inject(Router);

  readonly patientId = input.required<number>();
  /** Patient name, shown under the panel title. */
  readonly description = input('');

  /** A billing was registered or voided. */
  readonly changed = output<void>();

  protected readonly isDesktop = injectIsDesktop();
  protected readonly formatAmount = formatAmount;
  protected readonly formatLongDate = formatLongDate;
  protected readonly formatDate = formatDate;
  protected readonly sessionCountLabel = sessionCountLabel;

  protected readonly summary = signal<BillingSummary | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly expandedIds = signal<ReadonlySet<number>>(new Set());
  protected readonly confirmingVoidId = signal<number | null>(null);
  protected readonly voidingId = signal<number | null>(null);
  /** Number of the current panel opening, or null when the panel is closed. */
  private readonly panelOpening = signal<number | null>(null);
  private openings = 0;
  /** Message carried into the re-created form after a stale list. */
  protected readonly staleNotice = signal<string | null>(null);

  protected readonly panelOpen = computed(() => this.panelOpening() !== null);
  /** One entry per opening, so the form is re-created (all sessions checked again) instead of reading its input only once. */
  protected readonly panelTargets = computed(() => {
    const opening = this.panelOpening();
    return opening === null ? [] : [opening];
  });

  ngOnInit(): void {
    this.billingService.summary(this.patientId()).subscribe({
      next: (summary) => this.summary.set(summary),
      error: (err: unknown) => this.error.set(apiErrorMessage(err) ?? LOAD_ERROR),
    });
  }

  protected openRegister(): void {
    if (this.isDesktop()) {
      if (this.panelOpen()) {
        return;
      }
      this.staleNotice.set(null);
      this.reopenPanel();
    } else {
      void this.router.navigate(['/pacientes', this.patientId(), 'facturacion', 'nueva']);
    }
  }

  /** A new panel opening re-creates the form, so it reads the summary's current pending list. */
  private reopenPanel(): void {
    this.openings += 1;
    this.panelOpening.set(this.openings);
  }

  /** The form hit a stale pending list: reload the summary and re-create the form, keeping the message. */
  protected onStale(): void {
    this.billingService.summary(this.patientId()).subscribe({
      next: (summary) => {
        this.staleNotice.set(STALE_NOTICE);
        this.summary.set(summary);
        this.changed.emit();
        if (summary.pending.length === 0) {
          this.closePanel();
        } else {
          this.reopenPanel();
        }
      },
      error: (err: unknown) => this.error.set(apiErrorMessage(err) ?? LOAD_ERROR),
    });
  }

  protected closePanel(): void {
    this.panelOpening.set(null);
  }

  protected onSaved(summary: BillingSummary): void {
    this.panelOpening.set(null);
    this.summary.set(summary);
    this.changed.emit();
  }

  protected isExpanded(billing: Billing): boolean {
    return this.expandedIds().has(billing.id);
  }

  protected toggleSessions(billing: Billing): void {
    this.expandedIds.update((ids) => {
      const next = new Set(ids);
      if (next.has(billing.id)) {
        next.delete(billing.id);
      } else {
        next.add(billing.id);
      }
      return next;
    });
  }

  protected askVoid(billing: Billing): void {
    this.error.set(null);
    this.confirmingVoidId.set(billing.id);
  }

  protected cancelVoid(): void {
    this.confirmingVoidId.set(null);
  }

  protected confirmVoid(billing: Billing): void {
    if (this.voidingId() !== null) {
      return;
    }
    this.error.set(null);
    this.voidingId.set(billing.id);
    this.billingService.voidBilling(billing.id).subscribe({
      next: (summary) => {
        this.voidingId.set(null);
        this.confirmingVoidId.set(null);
        this.summary.set(summary);
        this.changed.emit();
      },
      error: (err: unknown) => {
        this.voidingId.set(null);
        this.error.set(apiErrorMessage(err) ?? VOID_ERROR);
      },
    });
  }
}
