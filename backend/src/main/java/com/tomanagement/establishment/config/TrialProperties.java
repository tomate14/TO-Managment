package com.tomanagement.establishment.config;

import java.time.Duration;

import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Trial period settings (TRIAL_DURATION_DAYS in .env).
 */
@Validated
@ConfigurationProperties(prefix = "app.trial")
public record TrialProperties(@Positive int durationDays) {

    public Duration trial() {
        return Duration.ofDays(durationDays);
    }
}
