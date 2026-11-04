package com.tomanagement.common.exception.handler;

import com.tomanagement.common.exception.AccessDeniedApplicationException;
import com.tomanagement.common.exception.AuthenticationFailedException;
import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.DuplicateResourceException;
import com.tomanagement.common.exception.InternalSystemException;
import com.tomanagement.common.exception.ResourceNotFoundException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only controller that triggers each error path handled by GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/test/errors")
class ExceptionProbeController {

    @GetMapping("/not-found")
    void notFound() {
        throw new ResourceNotFoundException();
    }

    @GetMapping("/duplicate")
    void duplicate() {
        throw new DuplicateResourceException();
    }

    @GetMapping("/business")
    void business() {
        throw new BusinessRuleException();
    }

    @GetMapping("/authentication")
    void authentication() {
        throw new AuthenticationFailedException();
    }

    @GetMapping("/access-denied")
    void accessDenied() {
        throw new AccessDeniedApplicationException();
    }

    @GetMapping("/unexpected")
    void unexpected() {
        throw new IllegalStateException("db password=secret");
    }

    @GetMapping("/internal")
    void internal() {
        throw new InternalSystemException(new IllegalStateException("db password=secret"));
    }

    @GetMapping("/bounded")
    void bounded(@RequestParam @Min(1) Integer value) {
        // Method parameter validation is the behavior under test.
    }

    @PostMapping("/validated")
    void validated(@Valid @RequestBody ProbeRequest request) {
        // Body validation is the behavior under test.
    }

    @GetMapping("/typed/{id}")
    void typed(@PathVariable Long id) {
        // Path variable conversion is the behavior under test.
    }

    @GetMapping("/param")
    void param(@RequestParam String required) {
        // Required parameter presence is the behavior under test.
    }

    @GetMapping("/header")
    void header(@RequestHeader String required) {
        // Required header presence is the behavior under test.
    }

    record ProbeRequest(@NotBlank String name) {
    }
}
