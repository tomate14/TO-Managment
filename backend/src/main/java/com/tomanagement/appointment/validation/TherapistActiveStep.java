package com.tomanagement.appointment.validation;

import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.ErrorCode;

import org.springframework.stereotype.Component;

/**
 * Chain step: appointments can only be booked with an active therapist.
 */
@Component
public class TherapistActiveStep extends AppointmentValidationStep {

    @Override
    protected void check(AppointmentValidationContext context) {
        if (!context.draft().therapist().isActive()) {
            throw new BusinessRuleException(ErrorCode.THERAPIST_INACTIVE);
        }
    }
}
