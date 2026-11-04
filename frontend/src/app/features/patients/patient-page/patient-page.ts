import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ageInYears, dominanceLabel, formatDate, formatDni, fullName, patientInitials, telHref } from '../patient-format';
import { Patient } from '../patients.models';
import { PatientsService } from '../patients.service';

const NOT_FOUND = 'Este paciente no existe.';
const LOAD_ERROR = 'No pudimos cargar el paciente. Intentá de nuevo en unos minutos.';
const NOT_FOUND_STATUS = 404;

/** H4.3 patient page: personal data and (from É5 on) the diagnoses. */
@Component({
  selector: 'app-patient-page',
  imports: [RouterLink],
  templateUrl: './patient-page.html',
  styleUrl: './patient-page.scss',
})
export class PatientPage implements OnInit {
  private readonly patientsService = inject(PatientsService);
  protected readonly patientId = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));

  protected readonly patient = signal<Patient | null>(null);
  protected readonly error = signal<string | null>(null);

  protected readonly summary = computed(() => {
    const patient = this.patient();
    return patient === null
      ? ''
      : `DNI ${formatDni(patient.dni)} · ${ageInYears(patient.birthDate)} años · ${dominanceLabel(patient.dominance)}`;
  });

  protected readonly fullName = fullName;
  protected readonly initials = patientInitials;
  protected readonly formatDate = formatDate;
  protected readonly telHref = telHref;

  ngOnInit(): void {
    this.patientsService.get(this.patientId).subscribe({
      next: (patient) => this.patient.set(patient),
      error: (err: unknown) =>
        this.error.set(err instanceof HttpErrorResponse && err.status === NOT_FOUND_STATUS ? NOT_FOUND : LOAD_ERROR),
    });
  }
}
