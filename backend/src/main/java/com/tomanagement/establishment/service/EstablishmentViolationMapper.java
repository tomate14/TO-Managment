package com.tomanagement.establishment.service;

import com.tomanagement.common.exception.ApplicationException;
import com.tomanagement.common.exception.DuplicateResourceException;
import com.tomanagement.common.exception.InternalSystemException;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Translates a database integrity violation into the matching application exception: only the known unique
 * indexes become conflicts, anything else is an internal error.
 */
public final class EstablishmentViolationMapper {

    private EstablishmentViolationMapper() {
        // Static utility.
    }

    public static ApplicationException map(DataIntegrityViolationException violation) {
        for (EstablishmentConstraint constraint : EstablishmentConstraint.values()) {
            if (violates(violation, constraint)) {
                return new DuplicateResourceException(constraint.getErrorCode());
            }
        }
        return new InternalSystemException(violation);
    }

    private static boolean violates(DataIntegrityViolationException violation, EstablishmentConstraint constraint) {
        if (violation.getCause() instanceof ConstraintViolationException hibernate
                && constraint.getIndexName().equals(hibernate.getConstraintName())) {
            return true;
        }
        String message = violation.getMostSpecificCause().getMessage();
        return message != null && message.contains(constraint.getIndexName());
    }
}
