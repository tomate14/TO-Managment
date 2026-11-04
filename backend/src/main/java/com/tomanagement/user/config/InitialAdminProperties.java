package com.tomanagement.user.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Initial admin account bound from ADMIN_EMAIL, ADMIN_PASSWORD and ADMIN_NAME (.env). Validated so the
 * backend refuses to start with missing values.
 */
@Validated
@ConfigurationProperties(prefix = "app.initial-admin")
public record InitialAdminProperties(@NotBlank @Email String email, @NotBlank String password, @NotBlank String name) {

    @Override
    public String toString() {
        return "InitialAdminProperties[email=***, password=***, name=***]";
    }
}
