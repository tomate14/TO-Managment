package com.tomanagement.registration.ratelimit;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import com.tomanagement.common.exception.TooManyRequestsException;
import com.tomanagement.registration.config.RegistrationProperties;

import org.springframework.stereotype.Component;

/**
 * Fixed-window counter per client, in memory: enough for one backend instance. Expired windows are evicted on every
 * call so the map stays bounded.
 */
@Component
public class FixedWindowRegistrationRateLimiter implements RegistrationRateLimiter {

    private record Window(Instant start, int count) {
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final int maxPerWindow;
    private final Clock clock;

    public FixedWindowRegistrationRateLimiter(RegistrationProperties properties, Clock clock) {
        this.maxPerWindow = properties.maxPerHour();
        this.clock = clock;
    }

    @Override
    public void acquire(String clientKey) {
        Instant now = clock.instant();
        windows.values().removeIf(window -> isExpired(window, now));
        Window updated = windows.compute(clientKey, (key, current) -> current == null || isExpired(current, now)
                ? new Window(now, 1)
                : new Window(current.start(), current.count() + 1));
        if (updated.count() > maxPerWindow) {
            throw new TooManyRequestsException();
        }
    }

    private static boolean isExpired(Window window, Instant now) {
        return !now.isBefore(window.start().plus(RateLimitWindow.HOUR.length()));
    }
}
