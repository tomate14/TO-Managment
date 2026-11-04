package com.tomanagement.user.dto;

import com.tomanagement.user.entity.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Editable fields of an existing user (the email cannot change).
 */
public record UpdateUserRequest(
        @NotBlank(message = "Ingresá el nombre.") @Size(max = 120, message = "El nombre es demasiado largo.")
        String fullName,
        @NotNull(message = "Elegí un rol.") Role role) {

    public UpdateUserRequest {
        fullName = fullName == null ? null : fullName.trim();
    }
}
