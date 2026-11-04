package com.tomanagement.auth.security;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only endpoints to exercise the security chain.
 */
@RestController
class SecurityProbeController {

    @GetMapping("/api/test/secured")
    String secured(@AuthenticationPrincipal AuthenticatedUser user) {
        return user.email();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/test/admin-only")
    String adminOnly() {
        return "ok";
    }
}
