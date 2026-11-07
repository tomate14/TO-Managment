package com.tomanagement.registration.config;

import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Public registration settings (REGISTRATION_MAX_PER_HOUR in .env).
 */
@Validated
@ConfigurationProperties(prefix = "app.registration")
public record RegistrationProperties(@Positive int maxPerHour) {
}
