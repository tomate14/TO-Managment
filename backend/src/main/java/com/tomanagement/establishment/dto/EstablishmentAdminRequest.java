package com.tomanagement.establishment.dto;

import com.tomanagement.user.entity.EmailNormalizer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * First admin of a new establishment. The email is normalized and the name trimmed; the password never appears in
 * {@link #toString()}.
 */
public record EstablishmentAdminRequest(
        @NotBlank(message = "Ingresá el nombre.") @Size(max = 120, message = "El nombre es demasiado largo.")
        String fullName,
        @NotBlank(message = "Ingresá el email.") @Email(message = "Ingresá un email válido.")
        @Size(max = 254, message = "El email es demasiado largo.") String email,
        @NotNull(message = "Ingresá la contraseña inicial.")
        @Size(min = 8, max = 72, message = "La contraseña tiene que tener entre 8 y 72 caracteres.") String password) {

    public EstablishmentAdminRequest {
        email = EmailNormalizer.normalize(email);
        fullName = fullName == null ? null : fullName.trim();
    }

    @Override
    public String toString() {
        return "EstablishmentAdminRequest[fullName=" + fullName + ", email=" + email + ", password=***]";
    }
}
