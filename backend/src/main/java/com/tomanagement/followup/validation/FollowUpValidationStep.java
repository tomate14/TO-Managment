package com.tomanagement.followup.validation;

/**
 * Chain of Responsibility: one link of the follow-up validation. Each step checks its own rule, throwing an
 * {@link com.tomanagement.common.exception.ApplicationException} on failure, and then hands over to the next one.
 */
public abstract class FollowUpValidationStep {

    private FollowUpValidationStep next;

    /**
     * Links {@code next} after this step.
     *
     * @return {@code next}, so links can be chained fluently
     */
    final FollowUpValidationStep then(FollowUpValidationStep next) {
        this.next = next;
        return next;
    }

    public final void validate(FollowUpValidationContext context) {
        check(context);
        if (next != null) {
            next.validate(context);
        }
    }

    protected abstract void check(FollowUpValidationContext context);
}
