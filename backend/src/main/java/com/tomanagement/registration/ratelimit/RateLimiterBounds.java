package com.tomanagement.registration.ratelimit;

import java.time.Duration;

/**
 * Memory bounds of the in-memory limiter: how many clients it tracks at most and how often it sweeps expired windows.
 */
enum RateLimiterBounds {

    DEFAULT(10_000, Duration.ofMinutes(1));

    private final int maxTrackedClients;
    private final Duration sweepInterval;

    RateLimiterBounds(int maxTrackedClients, Duration sweepInterval) {
        this.maxTrackedClients = maxTrackedClients;
        this.sweepInterval = sweepInterval;
    }

    int maxTrackedClients() {
        return maxTrackedClients;
    }

    Duration sweepInterval() {
        return sweepInterval;
    }
}
