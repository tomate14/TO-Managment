package com.tomanagement.common.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Single clock of the app, in APP_TIME_ZONE (.env): instants are zone-independent, but calendar dates the system
 * assigns (e.g. a diagnosis date) must follow the clinic's day, not UTC's.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.time-zone}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
