import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Patient } from '../patients.models';
import { PatientForm } from '../patient-form/patient-form';

/** H4.1 new patient (`/pacientes/nuevo`) and H4.3 edit (`/pacientes/:id/editar`, DNI locked). */
@Component({
  selector: 'app-patient-form-page',
  imports: [RouterLink, PatientForm],
  templateUrl: './patient-form-page.html',
  styleUrl: './patient-form-page.scss',
})
export class PatientFormPage {
  private readonly router = inject(Router);
  private readonly idParam = inject(ActivatedRoute).snapshot.paramMap.get('id');

  protected readonly patientId = this.idParam === null ? null : Number(this.idParam);
  protected readonly isEdit = this.patientId !== null;
  protected readonly backLink: unknown[] = this.isEdit ? ['/pacientes', this.patientId] : ['/pacientes'];

  protected onSaved(patient: Patient): void {
    void this.router.navigate(['/pacientes', patient.id], { replaceUrl: true });
  }
}
