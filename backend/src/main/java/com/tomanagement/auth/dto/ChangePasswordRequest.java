package com.tomanagement.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Own password change. Neither password appears in {@link #toString()}. 72 is BCrypt's input limit.
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Ingresá tu contraseña actual.") String currentPassword,
        @NotNull(message = "Ingresá la nueva contraseña.")
        @Size(min = 8, max = 72, message = "La nueva contraseña tiene que tener entre 8 y 72 caracteres.")
        String newPassword) {

    @Override
    public String toString() {
        return "ChangePasswordRequest[currentPassword=***, newPassword=***]";
    }
}
