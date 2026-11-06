package com.tomanagement.user.validation;

import com.tomanagement.user.entity.Role;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Checks {@link ValidManagedRole}: only roles assignable inside an establishment are accepted.
 */
public class ManagedRoleValidator implements ConstraintValidator<ValidManagedRole, Role> {

    @Override
    public boolean isValid(Role role, ConstraintValidatorContext context) {
        return role == null || role.isAssignableInEstablishment();
    }
}
