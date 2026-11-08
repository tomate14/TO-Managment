import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../../core/api/api-error';
import { Diagnosis } from '../../diagnoses/diagnoses.models';
import { DiagnosesService } from '../../diagnoses/diagnoses.service';
import { fullName } from '../../patients/patient-format';
import { BillingForm } from '../billing-form/billing-form';
import { BillingSummary } from '../billing.models';
import { BillingService } from '../billing.service';

const LOAD_ERROR = 'No pudimos cargar las sesiones pendientes. Intentá de nuevo en unos minutos.';

/**
 * H12.2 mobile: register a billing (`/pacientes/:id/diagnosticos/:diagnosisId/facturacion/nueva`).
 * Desktop uses the side panel of the diagnosis' billing section instead.
 */
@Component({
  selector: 'app-billing-form-page',
  imports: [RouterLink, BillingForm],
  templateUrl: './billing-form-page.html',
  styleUrl: './billing-form-page.scss',
})
export class BillingFormPage {
  private readonly billingService = inject(BillingService);
  private readonly diagnosesService = inject(DiagnosesService);
  private readonly router = inject(Router);
  private readonly params = inject(ActivatedRoute).snapshot.paramMap;

  protected readonly patientId = Number(this.params.get('id'));
  protected readonly diagnosisId = Number(this.params.get('diagnosisId'));

  protected readonly diagnosis = signal<Diagnosis | null>(null);
  protected readonly summary = signal<BillingSummary | null>(null);
  protected readonly error = signal<string | null>(null);

  protected readonly subtitle = computed(() => {
    const diagnosis = this.diagnosis();
    return diagnosis ? `${diagnosis.description} · ${fullName(diagnosis.patient)}` : '';
  });

  constructor() {
    this.diagnosesService.get(this.diagnosisId).subscribe({
      next: (diagnosis) => this.diagnosis.set(diagnosis),
      error: () => this.diagnosis.set(null),
    });
    this.billingService.summary(this.diagnosisId).subscribe({
      next: (summary) => this.summary.set(summary),
      error: (err: unknown) => this.error.set(apiErrorMessage(err) ?? LOAD_ERROR),
    });
  }

  protected backToDiagnosis(): void {
    void this.router.navigate(['/pacientes', this.patientId, 'diagnosticos', this.diagnosisId]);
  }
}
