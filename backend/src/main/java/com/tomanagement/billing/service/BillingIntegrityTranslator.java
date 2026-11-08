package com.tomanagement.billing.service;

import com.tomanagement.common.exception.ApplicationException;
import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.InternalSystemException;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Translates a database integrity failure while billing: only the race on the active-link index is a session
 * error; any other integrity failure is internal.
 */
final class BillingIntegrityTranslator {

    private BillingIntegrityTranslator() {
    }

    static ApplicationException translate(DataIntegrityViolationException violation) {
        for (BillingConstraint constraint : BillingConstraint.values()) {
            if (violates(violation, constraint)) {
                return new BusinessRuleException(constraint.getErrorCode());
            }
        }
        return new InternalSystemException(violation);
    }

    private static boolean violates(DataIntegrityViolationException violation, BillingConstraint constraint) {
        if (violation.getCause() instanceof ConstraintViolationException hibernate
                && constraint.getIndexName().equals(hibernate.getConstraintName())) {
            return true;
        }
        String message = violation.getMostSpecificCause().getMessage();
        return message != null && message.contains(constraint.getIndexName());
    }
}
