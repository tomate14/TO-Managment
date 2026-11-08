package com.tomanagement.diagnosis.mapper;

import com.tomanagement.catalog.dto.CatalogItemResponse;
import com.tomanagement.diagnosis.dto.DiagnosisResponse;
import com.tomanagement.diagnosis.dto.PatientRef;
import com.tomanagement.diagnosis.dto.TreatmentResponse;
import com.tomanagement.diagnosis.entity.ConservativeTreatment;
import com.tomanagement.diagnosis.entity.Diagnosis;
import com.tomanagement.diagnosis.entity.SurgicalTreatment;
import com.tomanagement.diagnosis.entity.TreatmentVisitor;

import org.springframework.stereotype.Component;

/**
 * Entity → DTO mapping for diagnoses; the treatment is read with a {@link TreatmentVisitor}.
 */
@Component
public class DiagnosisMapper {

    private static final TreatmentVisitor<TreatmentResponse> TREATMENT_TO_RESPONSE = new TreatmentVisitor<>() {
        @Override
        public TreatmentResponse visitSurgical(SurgicalTreatment treatment) {
            return new TreatmentResponse(treatment.getType(), treatment.getSurgeryDate(),
                    treatment.getStitchesRemovalDate(), CatalogItemResponse.from(treatment.getSurgeryType()), null, null,
                    treatment.requiresScarType());
        }

        @Override
        public TreatmentResponse visitConservative(ConservativeTreatment treatment) {
            return new TreatmentResponse(treatment.getType(), null, null, null, treatment.getImmobilizationDate(),
                    treatment.hasImmobilization(), treatment.requiresScarType());
        }
    };

    public DiagnosisResponse toResponse(Diagnosis diagnosis, long followUpCount, long pendingBillingCount) {
        return new DiagnosisResponse(diagnosis.getId(), PatientRef.from(diagnosis.getPatient()),
                diagnosis.getDescription(), diagnosis.getCreatedDate(),
                diagnosis.getTreatment().accept(TREATMENT_TO_RESPONSE), followUpCount,
                pendingBillingCount);
    }
}
