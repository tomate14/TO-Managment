import { CatalogItem } from '../catalogs/catalogs.models';

export type TreatmentType = 'SURGICAL' | 'CONSERVATIVE';

export interface PatientRef {
  id: number;
  firstName: string;
  lastName: string;
  dni: string;
}

export interface Treatment {
  type: TreatmentType;
  surgeryDate: string | null;
  stitchesRemovalDate: string | null;
  surgeryType: CatalogItem | null;
  /** Null for surgical treatments. */
  hasImmobilization: boolean | null;
  /** Null when there is no immobilization. */
  immobilizationDate: string | null;
  requiresScarType: boolean;
}

export interface Diagnosis {
  id: number;
  patient: PatientRef;
  description: string;
  /** ISO date assigned by the system. */
  createdDate: string;
  treatment: Treatment;
  followUpCount: number;
  /** Sessions (follow-ups) of this diagnosis not yet billed. */
  pendingBillingCount: number;
}

export interface CreateDiagnosisPayload {
  description: string;
  treatment: {
    type: TreatmentType;
    surgeryDate: string | null;
    stitchesRemovalDate: string | null;
    surgeryTypeId: number | null;
    immobilizationDate: string | null;
    /** Null for surgical treatments. */
    hasImmobilization: boolean | null;
  };
}

export const TREATMENT_OPTIONS: readonly { value: TreatmentType; label: string; hint: string }[] = [
  { value: 'SURGICAL', label: 'Quirúrgico', hint: 'Con cirugía. Pide fechas y tipo de cirugía.' },
  { value: 'CONSERVATIVE', label: 'Conservador', hint: 'Sin cirugía. Indicá si lleva inmovilización.' },
];

export function treatmentLabel(type: TreatmentType): string {
  return TREATMENT_OPTIONS.find((option) => option.value === type)?.label ?? type;
}

export function immobilizationLabel(treatment: Treatment, format: (iso: string) => string): string {
  return treatment.hasImmobilization === false || treatment.immobilizationDate === null
    ? 'Sin inmovilización'
    : format(treatment.immobilizationDate);
}
