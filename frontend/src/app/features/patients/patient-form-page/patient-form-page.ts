import { Component, OnInit, inject, signal } from '@angular/core';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { apiErrorCode, apiErrorMessage } from '../../../core/api/api-error';
import { normalizeDni, todayIso } from '../patient-format';
import { DOMINANCE_OPTIONS, Dominance, PATIENT_ERROR_CODES, PatientData } from '../patients.models';
import { PatientsService } from '../patients.service';

type TextField = 'dni' | 'firstName' | 'lastName' | 'birthDate' | 'phone' | 'occupation' | 'healthInsurance'
  | 'healthInsuranceNumber';

const REQUIRED_MESSAGES: Record<TextField, string> = {
  dni: 'Ingresá el DNI.',
  firstName: 'Ingresá el nombre.',
  lastName: 'Ingresá el apellido.',
  birthDate: 'Ingresá la fecha de nacimiento.',
  phone: 'Ingresá el teléfono.',
  occupation: 'Ingresá la ocupación.',
  healthInsurance: 'Ingresá la obra social.',
  healthInsuranceNumber: 'Ingresá el número de afiliado.',
};
const DUPLICATE_DNI = 'Ya existe un paciente con este DNI.';
const SAVE_ERROR = 'No pudimos guardar el paciente. Intentá de nuevo.';
const LOAD_ERROR = 'No pudimos cargar el paciente.';

function notBlank(control: AbstractControl<string>): ValidationErrors | null {
  return control.value.trim() === '' ? { required: true } : null;
}

/** 6 to 9 digits once dots and spaces are removed (same rule as the backend). */
function dniFormat(control: AbstractControl<string>): ValidationErrors | null {
  const digits = normalizeDni(control.value);
  return digits === '' || /^\d{6,9}$/.test(digits) ? null : { dni: true };
}

function notFuture(control: AbstractControl<string>): ValidationErrors | null {
  return control.value !== '' && control.value > todayIso() ? { future: true } : null;
}

/** H4.1 new patient (`/pacientes/nuevo`) and H4.3 edit (`/pacientes/:id/editar`, DNI locked). */
@Component({
  selector: 'app-patient-form-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './patient-form-page.html',
  styleUrl: './patient-form-page.scss',
})
export class PatientFormPage implements OnInit {
  private readonly patientsService = inject(PatientsService);
  private readonly router = inject(Router);
  private readonly idParam = inject(ActivatedRoute).snapshot.paramMap.get('id');

  protected readonly patientId = this.idParam === null ? null : Number(this.idParam);
  protected readonly isEdit = this.patientId !== null;
  protected readonly today = todayIso();
  protected readonly dominanceOptions = DOMINANCE_OPTIONS;

  protected readonly form = inject(NonNullableFormBuilder).group({
    dni: ['', [notBlank, dniFormat]],
    firstName: ['', [notBlank, Validators.maxLength(80)]],
    lastName: ['', [notBlank, Validators.maxLength(80)]],
    birthDate: ['', [Validators.required, notFuture]],
    phone: ['', [notBlank, Validators.maxLength(40)]],
    occupation: ['', [notBlank, Validators.maxLength(120)]],
    healthInsurance: ['', [notBlank, Validators.maxLength(120)]],
    healthInsuranceNumber: ['', [notBlank, Validators.maxLength(60)]],
    dominance: ['RIGHT' as Dominance],
  });

  protected readonly submitted = signal(false);
  protected readonly saving = signal(false);
  protected readonly loading = signal(this.isEdit);
  protected readonly dniTaken = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    if (this.isEdit) {
      this.form.controls.dni.disable();
    }
    this.form.controls.dni.valueChanges.subscribe(() => this.dniTaken.set(false));
  }

  ngOnInit(): void {
    if (this.patientId === null) {
      return;
    }
    this.patientsService.get(this.patientId).subscribe({
      next: (patient) => {
        this.form.patchValue(patient);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set(LOAD_ERROR);
      },
    });
  }

  protected selectDominance(dominance: Dominance): void {
    this.form.controls.dominance.setValue(dominance);
  }

  protected fieldError(field: TextField): string | null {
    if (field === 'dni' && this.dniTaken()) {
      return DUPLICATE_DNI;
    }
    const control = this.form.controls[field];
    if (control.disabled || (!this.submitted() && !control.touched) || control.valid) {
      return null;
    }
    if (control.hasError('required')) {
      return REQUIRED_MESSAGES[field];
    }
    if (control.hasError('dni')) {
      return 'El DNI tiene que tener entre 6 y 9 números.';
    }
    if (control.hasError('future')) {
      return 'La fecha de nacimiento no puede ser futura.';
    }
    return 'Es demasiado largo.';
  }

  protected submit(): void {
    if (this.saving() || this.loading()) {
      return;
    }
    this.submitted.set(true);
    this.error.set(null);
    if (this.form.invalid) {
      return;
    }
    this.saving.set(true);
    const value = this.form.getRawValue();
    const data: PatientData = {
      firstName: value.firstName.trim(),
      lastName: value.lastName.trim(),
      birthDate: value.birthDate,
      phone: value.phone.trim(),
      occupation: value.occupation.trim(),
      healthInsurance: value.healthInsurance.trim(),
      healthInsuranceNumber: value.healthInsuranceNumber.trim(),
      dominance: value.dominance,
    };
    const request = this.patientId === null
      ? this.patientsService.create({ ...data, dni: normalizeDni(value.dni) })
      : this.patientsService.update(this.patientId, data);
    request.subscribe({
      next: (saved) => {
        this.saving.set(false);
        void this.router.navigate(['/pacientes', saved.id], { replaceUrl: true });
      },
      error: (err: unknown) => {
        this.saving.set(false);
        if (apiErrorCode(err) === PATIENT_ERROR_CODES.dniDuplicate) {
          this.dniTaken.set(true);
        } else {
          this.error.set(apiErrorMessage(err) ?? SAVE_ERROR);
        }
      },
    });
  }
}
