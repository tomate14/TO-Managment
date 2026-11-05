package com.tomanagement.appointment.validation;

/**
 * Chain of Responsibility: one link of the booking validation. Each step checks its own rule, throwing an
 * {@link com.tomanagement.common.exception.ApplicationException} on failure, and then hands over to the next one.
 */
public abstract class AppointmentValidationStep {

    private AppointmentValidationStep next;

    /**
     * Links {@code next} after this step.
     *
     * @return {@code next}, so links can be chained fluently
     */
    final AppointmentValidationStep then(AppointmentValidationStep next) {
        this.next = next;
        return next;
    }

    public final void validate(AppointmentValidationContext context) {
        check(context);
        if (next != null) {
            next.validate(context);
        }
    }

    protected abstract void check(AppointmentValidationContext context);
}
