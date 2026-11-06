package com.tomanagement.user.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * The role must be one an establishment can assign (Admin or Terapeuta), never SUPERADMIN. A null role is left to
 * {@code @NotNull}.
 */
@Documented
@Constraint(validatedBy = ManagedRoleValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidManagedRole {

    String message() default "Elegí Admin o Terapeuta.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
