import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { formatDate } from '../../patients/patient-format';
import { followUpCountLabel, pendingBillingLabel } from '../../follow-ups/follow-up-format';
import { Diagnosis, immobilizationLabel, treatmentLabel } from '../diagnoses.models';

/** Diagnosis summary on the patient page (H5.2): description, treatment badge and its key data. */
@Component({
  selector: 'app-diagnosis-card',
  imports: [RouterLink],
  templateUrl: './diagnosis-card.html',
  styleUrl: './diagnosis-card.scss',
})
export class DiagnosisCard {
  readonly diagnosis = input.required<Diagnosis>();

  protected readonly label = computed(() => treatmentLabel(this.diagnosis().treatment.type));
  protected readonly isSurgical = computed(() => this.diagnosis().treatment.type === 'SURGICAL');
  protected readonly link = computed(() => ['/pacientes', this.diagnosis().patient.id, 'diagnosticos', this.diagnosis().id]);
  protected readonly countLabel = computed(() => followUpCountLabel(this.diagnosis().followUpCount));
  protected readonly pendingLabel = computed(() => pendingBillingLabel(this.diagnosis().pendingBillingCount));
  protected readonly formatDate = formatDate;
  protected readonly immobilizationLabel = immobilizationLabel;
}
