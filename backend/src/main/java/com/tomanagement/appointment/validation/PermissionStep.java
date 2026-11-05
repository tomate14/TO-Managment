package com.tomanagement.appointment.validation;

import com.tomanagement.appointment.visibility.AppointmentVisibilityPolicyFactory;

import org.springframework.stereotype.Component;

/**
 * Chain step: the actor must be allowed to manage the therapist's agenda (delegates to the role's policy).
 */
@Component
public class PermissionStep extends AppointmentValidationStep {

    private final AppointmentVisibilityPolicyFactory policyFactory;

    public PermissionStep(AppointmentVisibilityPolicyFactory policyFactory) {
        this.policyFactory = policyFactory;
    }

    @Override
    protected void check(AppointmentValidationContext context) {
        policyFactory.forRole(context.actor().role())
                .checkCanManage(context.actor(), context.draft().therapist().getId());
    }
}
