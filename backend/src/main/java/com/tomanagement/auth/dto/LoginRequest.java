package com.tomanagement.auth.dto;

import com.tomanagement.user.entity.EmailNormalizer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Login credentials. toString never reveals the password.
 */
public record LoginRequest(
        @NotBlank(message = "Ingresá tu email.") @Email(message = "Ingresá un email válido.") String email,
        @NotBlank(message = "Ingresá tu contraseña.") String password) {

    /** Normalizes the email before bean validation runs, so surrounding whitespace is not rejected. */
    public LoginRequest {
        email = EmailNormalizer.normalize(email);
    }

    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=***]";
    }
}
