package com.tomanagement.registration.ratelimit;

import com.tomanagement.common.exception.TooManyRequestsException;

public interface RegistrationRateLimiter {

    /**
     * Counts one registration attempt for the client.
     *
     * @throws TooManyRequestsException when the client exceeded its allowance
     */
    void acquire(String clientKey);
}
