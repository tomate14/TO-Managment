package com.tomanagement.appointment.validation;

import org.springframework.stereotype.Component;

/**
 * Chain of Responsibility: schedule, permission, active therapist, diagnosis ownership, therapist free, patient
 * free. The first failing step stops the chain.
 */
@Component
public class AppointmentValidationChain {

    private final AppointmentValidationStep first;

    public AppointmentValidationChain(ScheduleStep schedule, PermissionStep permission,
            TherapistActiveStep therapistActive, DiagnosisOwnershipStep diagnosisOwnership,
            TherapistFreeStep therapistFree, PatientFreeStep patientFree) {
        schedule.then(permission).then(therapistActive).then(diagnosisOwnership).then(therapistFree)
                .then(patientFree);
        this.first = schedule;
    }

    public void validate(AppointmentValidationContext context) {
        first.validate(context);
    }
}
