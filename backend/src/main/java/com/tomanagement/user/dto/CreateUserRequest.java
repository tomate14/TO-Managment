package com.tomanagement.user.dto;

import com.tomanagement.user.entity.EmailNormalizer;
import com.tomanagement.user.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * New account data. The email is normalized and the name trimmed before validation; the password never
 * appears in {@link #toString()}. 72 is BCrypt's input limit.
 */
public record CreateUserRequest(
        @NotBlank(message = "Ingresá el email.") @Email(message = "Ingresá un email válido.")
        @Size(max = 254, message = "El email es demasiado largo.") String email,
        @NotBlank(message = "Ingresá el nombre.") @Size(max = 120, message = "El nombre es demasiado largo.")
        String fullName,
        @NotNull(message = "Ingresá la contraseña inicial.")
        @Size(min = 8, max = 72, message = "La contraseña tiene que tener entre 8 y 72 caracteres.") String password,
        @NotNull(message = "Elegí un rol.") Role role) {

    public CreateUserRequest {
        email = EmailNormalizer.normalize(email);
        fullName = fullName == null ? null : fullName.trim();
    }

    @Override
    public String toString() {
        return "CreateUserRequest[email=" + email + ", fullName=" + fullName + ", password=***, role=" + role + "]";
    }
}
