package com.tomanagement.registration.ratelimit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import com.tomanagement.common.exception.TooManyRequestsException;
import com.tomanagement.registration.config.RegistrationProperties;
import com.tomanagement.support.MutableClock;

import org.junit.jupiter.api.Test;

class FixedWindowRegistrationRateLimiterTest {

    @Test
    void blocksAfterTheMaximumAndResetsAfterAnHour() {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-06T12:00:00Z"));
        RegistrationRateLimiter limiter = new FixedWindowRegistrationRateLimiter(new RegistrationProperties(2), clock);
        limiter.acquire("1.2.3.4");
        limiter.acquire("1.2.3.4");
        assertThatThrownBy(() -> limiter.acquire("1.2.3.4"))
                .isInstanceOf(TooManyRequestsException.class);
        limiter.acquire("5.6.7.8"); // other IPs are independent
        clock.advance(Duration.ofHours(1));
        limiter.acquire("1.2.3.4"); // new window
    }
}
