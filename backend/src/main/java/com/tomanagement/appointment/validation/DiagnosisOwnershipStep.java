package com.tomanagement.appointment.validation;

import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.diagnosis.entity.Diagnosis;

import org.springframework.stereotype.Component;

/**
 * Chain step: an optional diagnosis must belong to the patient of the appointment.
 */
@Component
public class DiagnosisOwnershipStep extends AppointmentValidationStep {

    @Override
    protected void check(AppointmentValidationContext context) {
        AppointmentDraft draft = context.draft();
        Diagnosis diagnosis = draft.diagnosis();
        if (diagnosis != null && !diagnosis.getPatient().getId().equals(draft.patient().getId())) {
            throw new BusinessRuleException(ErrorCode.DIAGNOSIS_NOT_OF_PATIENT);
        }
    }
}
