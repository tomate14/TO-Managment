import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../../core/api/api-error';
import { CatalogItem } from '../../catalogs/catalogs.models';
import { CatalogsService } from '../../catalogs/catalogs.service';
import { formatDni, fullName } from '../../patients/patient-format';
import { Patient } from '../../patients/patients.models';
import { PatientsService } from '../../patients/patients.service';
import { CreateDiagnosisPayload, TREATMENT_OPTIONS, TreatmentType } from '../diagnoses.models';
import { DiagnosesService } from '../diagnoses.service';

interface Problems {
  description?: string;
  surgeryDate?: string;
  stitchesRemovalDate?: string;
  surgeryTypeId?: string;
  immobilizationDate?: string;
}

const SAVE_ERROR = 'No pudimos crear el diagnóstico. Intentá de nuevo.';

/** H5.1 new diagnosis (`/pacientes/:id/diagnosticos/nuevo`): surgical or conservative treatment. */
@Component({
  selector: 'app-diagnosis-form-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './diagnosis-form-page.html',
  styleUrl: './diagnosis-form-page.scss',
})
export class DiagnosisFormPage implements OnInit {
  private readonly diagnosesService = inject(DiagnosesService);
  private readonly patientsService = inject(PatientsService);
  private readonly catalogsService = inject(CatalogsService);
  private readonly router = inject(Router);

  protected readonly patientId = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  protected readonly treatmentOptions = TREATMENT_OPTIONS;

  protected readonly form = inject(NonNullableFormBuilder).group({
    description: [''],
    type: ['SURGICAL' as TreatmentType],
    surgeryDate: [''],
    stitchesRemovalDate: [''],
    surgeryTypeId: [null as number | null],
    immobilizationDate: [''],
  });

  protected readonly patient = signal<Patient | null>(null);
  protected readonly surgeryTypes = signal<CatalogItem[]>([]);
  protected readonly submitted = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  private readonly value = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });
  protected readonly type = computed(() => this.value().type ?? 'SURGICAL');
  protected readonly problems = computed(() => this.validate());
  protected readonly subtitle = computed(() => {
    const patient = this.patient();
    return patient === null ? '' : `${fullName(patient)} · DNI ${formatDni(patient.dni)}`;
  });

  ngOnInit(): void {
    this.patientsService.get(this.patientId).subscribe({ next: (patient) => this.patient.set(patient), error: () => undefined });
    this.catalogsService.list('surgery-types', true).subscribe({
      next: (types) => this.surgeryTypes.set(types),
      error: () => this.surgeryTypes.set([]),
    });
  }

  protected selectType(type: TreatmentType): void {
    this.form.controls.type.setValue(type);
  }

  protected submit(): void {
    if (this.saving()) {
      return;
    }
    this.submitted.set(true);
    this.error.set(null);
    if (Object.keys(this.problems()).length > 0) {
      return;
    }
    this.saving.set(true);
    this.diagnosesService.create(this.patientId, this.payload()).subscribe({
      next: (created) => {
        this.saving.set(false);
        void this.router.navigate(['/pacientes', this.patientId, 'diagnosticos', created.id], { replaceUrl: true });
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.error.set(apiErrorMessage(err) ?? SAVE_ERROR);
      },
    });
  }

  private validate(): Problems {
    const value = this.value();
    const problems: Problems = {};
    if ((value.description ?? '').trim() === '') {
      problems.description = 'Ingresá la descripción del diagnóstico.';
    }
    if (this.type() === 'SURGICAL') {
      const surgery = value.surgeryDate ?? '';
      const stitches = value.stitchesRemovalDate ?? '';
      if (surgery === '') {
        problems.surgeryDate = 'Ingresá la fecha de cirugía.';
      }
      if (stitches === '') {
        problems.stitchesRemovalDate = 'Ingresá la fecha de retiro de puntos.';
      } else if (surgery !== '' && stitches < surgery) {
        problems.stitchesRemovalDate = 'El retiro de puntos no puede ser anterior a la cirugía.';
      }
      if (value.surgeryTypeId === null || value.surgeryTypeId === undefined) {
        problems.surgeryTypeId = 'Elegí el tipo de cirugía.';
      }
    } else if ((value.immobilizationDate ?? '') === '') {
      problems.immobilizationDate = 'Ingresá la fecha de inmovilización.';
    }
    return problems;
  }

  private payload(): CreateDiagnosisPayload {
    const value = this.form.getRawValue();
    const surgical = value.type === 'SURGICAL';
    return {
      description: value.description.trim(),
      treatment: {
        type: value.type,
        surgeryDate: surgical ? value.surgeryDate : null,
        stitchesRemovalDate: surgical ? value.stitchesRemovalDate : null,
        surgeryTypeId: surgical ? Number(value.surgeryTypeId) : null,
        immobilizationDate: surgical ? null : value.immobilizationDate,
      },
    };
  }
}
