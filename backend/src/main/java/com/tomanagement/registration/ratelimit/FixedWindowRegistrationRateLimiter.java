package com.tomanagement.registration.ratelimit;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.tomanagement.common.exception.TooManyRequestsException;
import com.tomanagement.registration.config.RegistrationProperties;

import org.springframework.stereotype.Component;

/**
 * Fixed-window counter per client, in memory: enough for one backend instance. Memory is bounded: expired windows are
 * swept at most once per sweep interval and whenever the tracked-client cap is hit; if the cap still holds after a
 * sweep, new clients are refused (fail closed).
 */
@Component
public class FixedWindowRegistrationRateLimiter implements RegistrationRateLimiter {

    private record Window(Instant start, int count) {
    }

    private final Map<String, Window> windows = new HashMap<>();
    private final int maxPerWindow;
    private final Clock clock;
    private final int maxTrackedClients;
    private Instant lastSweep;

    public FixedWindowRegistrationRateLimiter(RegistrationProperties properties, Clock clock) {
        this(properties, clock, RateLimiterBounds.DEFAULT.maxTrackedClients());
    }

    FixedWindowRegistrationRateLimiter(RegistrationProperties properties, Clock clock, int maxTrackedClients) {
        this.maxPerWindow = properties.maxPerHour();
        this.clock = clock;
        this.maxTrackedClients = maxTrackedClients;
        this.lastSweep = clock.instant();
    }

    @Override
    public synchronized void acquire(String clientKey) {
        Instant now = clock.instant();
        if (!now.isBefore(lastSweep.plus(RateLimiterBounds.DEFAULT.sweepInterval()))) {
            sweep(now);
        }
        Window current = windows.get(clientKey);
        if (current == null && windows.size() >= maxTrackedClients) {
            sweep(now);
            if (windows.size() >= maxTrackedClients) {
                throw new TooManyRequestsException();
            }
        }
        Window updated = current == null || isExpired(current, now)
                ? new Window(now, 1)
                : new Window(current.start(), current.count() + 1);
        windows.put(clientKey, updated);
        if (updated.count() > maxPerWindow) {
            throw new TooManyRequestsException();
        }
    }

    private void sweep(Instant now) {
        windows.values().removeIf(window -> isExpired(window, now));
        lastSweep = now;
    }

    private static boolean isExpired(Window window, Instant now) {
        return !now.isBefore(window.start().plus(RateLimitWindow.HOUR.length()));
    }
}
