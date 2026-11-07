package com.tomanagement.registration.controller;

import com.tomanagement.registration.dto.RegisterTherapistRequest;
import com.tomanagement.registration.ratelimit.ClientIpResolver;
import com.tomanagement.registration.ratelimit.RegistrationRateLimiter;
import com.tomanagement.registration.service.TherapistRegistrationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RegistrationController {

    private final TherapistRegistrationService registrationService;
    private final RegistrationRateLimiter rateLimiter;
    private final ClientIpResolver ipResolver;

    public RegistrationController(TherapistRegistrationService registrationService,
            RegistrationRateLimiter rateLimiter, ClientIpResolver ipResolver) {
        this.registrationService = registrationService;
        this.rateLimiter = rateLimiter;
        this.ipResolver = ipResolver;
    }

    /** Spring validates the body before this runs, so invalid bodies are not counted by the limiter. */
    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(HttpServletRequest request, @Valid @RequestBody RegisterTherapistRequest body) {
        rateLimiter.acquire(ipResolver.resolve(request));
        registrationService.register(body);
    }
}
