package com.tomanagement.appointment.validation;

import com.tomanagement.appointment.repository.AppointmentRepository;
import com.tomanagement.common.exception.DuplicateResourceException;
import com.tomanagement.common.exception.ErrorCode;

import org.springframework.stereotype.Component;

/**
 * Chain step: the patient must have no overlapping appointment.
 */
@Component
public class PatientFreeStep extends AppointmentValidationStep {

    private final AppointmentRepository repository;

    public PatientFreeStep(AppointmentRepository repository) {
        this.repository = repository;
    }

    @Override
    protected void check(AppointmentValidationContext context) {
        AppointmentDraft draft = context.draft();
        if (repository.existsOverlapForPatient(draft.patient().getId(), draft.startAt(), draft.endAt(),
                draft.excludeAppointmentId())) {
            throw new DuplicateResourceException(ErrorCode.PATIENT_BUSY);
        }
    }
}
