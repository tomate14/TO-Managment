import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { formatDate, formatDni, fullName } from '../../patients/patient-format';
import { Diagnosis, immobilizationLabel, treatmentLabel } from '../diagnoses.models';
import { DiagnosesService } from '../diagnoses.service';
import { FollowUpCard } from '../../follow-ups/follow-up-card/follow-up-card';
import { FollowUp } from '../../follow-ups/follow-ups.models';
import { FollowUpsService } from '../../follow-ups/follow-ups.service';

const NOT_FOUND = 'Este diagnóstico no existe.';
const LOAD_ERROR = 'No pudimos cargar el diagnóstico. Intentá de nuevo en unos minutos.';
const FOLLOW_UPS_ERROR = 'No pudimos cargar los seguimientos. Intentá de nuevo en unos minutos.';
const NOT_FOUND_STATUS = 404;

/** H5.2 / H6.4 diagnosis detail (`/pacientes/:id/diagnosticos/:diagnosisId`) with its follow-up history. */
@Component({
  selector: 'app-diagnosis-page',
  imports: [RouterLink, FollowUpCard],
  templateUrl: './diagnosis-page.html',
  styleUrl: './diagnosis-page.scss',
})
export class DiagnosisPage implements OnInit {
  private readonly diagnosesService = inject(DiagnosesService);
  private readonly followUpsService = inject(FollowUpsService);
  private readonly params = inject(ActivatedRoute).snapshot.paramMap;

  protected readonly patientId = Number(this.params.get('id'));
  private readonly diagnosisId = Number(this.params.get('diagnosisId'));

  protected readonly diagnosis = signal<Diagnosis | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly followUps = signal<FollowUp[] | null>(null);
  protected readonly followUpsError = signal<string | null>(null);

  protected readonly subtitle = computed(() => {
    const patient = this.diagnosis()?.patient;
    return patient ? `${fullName(patient)} · DNI ${formatDni(patient.dni)}` : '';
  });
  protected readonly label = computed(() => {
    const diagnosis = this.diagnosis();
    return diagnosis ? treatmentLabel(diagnosis.treatment.type) : '';
  });
  protected readonly formatDate = formatDate;
  protected readonly immobilizationLabel = immobilizationLabel;

  ngOnInit(): void {
    this.diagnosesService.get(this.diagnosisId).subscribe({
      next: (diagnosis) => this.diagnosis.set(diagnosis),
      error: (err: unknown) =>
        this.error.set(err instanceof HttpErrorResponse && err.status === NOT_FOUND_STATUS ? NOT_FOUND : LOAD_ERROR),
    });
    this.followUpsService.listForDiagnosis(this.diagnosisId).subscribe({
      next: (followUps) => this.followUps.set(followUps),
      error: () => this.followUpsError.set(FOLLOW_UPS_ERROR),
    });
  }
}
