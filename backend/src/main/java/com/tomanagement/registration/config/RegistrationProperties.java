package com.tomanagement.registration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Public registration settings (REGISTRATION_MAX_PER_HOUR in .env).
 */
@ConfigurationProperties(prefix = "app.registration")
public record RegistrationProperties(int maxPerHour) {
}
