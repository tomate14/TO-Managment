package com.tomanagement.registration.ratelimit;

import java.time.Duration;

/**
 * Length of the rate-limit window (the limit itself is "per hour", see REGISTRATION_MAX_PER_HOUR).
 */
enum RateLimitWindow {

    HOUR(Duration.ofHours(1));

    private final Duration length;

    RateLimitWindow(Duration length) {
        this.length = length;
    }

    Duration length() {
        return length;
    }
}
