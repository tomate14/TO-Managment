package com.tomanagement.registration.dto;

import com.tomanagement.user.entity.EmailNormalizer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Self-registration of an independent therapist.
 */
public record RegisterTherapistRequest(
        @NotBlank(message = "Ingresá tu nombre.") @Size(max = 120, message = "El nombre es demasiado largo.") String fullName,
        @NotBlank(message = "Ingresá tu email.") @Email(message = "Ingresá un email válido.")
        @Size(max = 254, message = "El email es demasiado largo.") String email,
        @NotNull(message = "Ingresá una contraseña.")
        @Size(min = 8, max = 72, message = "La contraseña tiene que tener entre 8 y 72 caracteres.") String password) {

    public RegisterTherapistRequest {
        email = EmailNormalizer.normalize(email);
        fullName = fullName == null ? null : fullName.trim();
    }

    @Override
    public String toString() {
        return "RegisterTherapistRequest[fullName=***, email=***, password=***]";
    }
}
