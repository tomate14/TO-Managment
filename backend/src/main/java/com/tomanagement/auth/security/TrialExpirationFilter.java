package com.tomanagement.auth.security;

import java.io.IOException;

import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.establishment.service.TrialStatusChecker;
import com.tomanagement.user.entity.Role;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Cuts off authenticated requests of an establishment whose trial ended (403 TRIAL_EXPIRED), so an open session
 * stops on its next action. The superadmin is never blocked, not even inside an expired establishment.
 * Not a Spring bean on purpose: SecurityConfig adds it right after JWT authentication.
 */
public class TrialExpirationFilter extends OncePerRequestFilter {

    private final TrialStatusChecker trialStatusChecker;
    private final ErrorResponseWriter errorResponseWriter;

    public TrialExpirationFilter(TrialStatusChecker trialStatusChecker, ErrorResponseWriter errorResponseWriter) {
        this.trialStatusChecker = trialStatusChecker;
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user
                && user.role() != Role.SUPERADMIN && user.establishmentId() != null
                && trialStatusChecker.isExpired(user.establishmentId())) {
            SecurityContextHolder.clearContext();
            errorResponseWriter.write(response, ErrorCode.TRIAL_EXPIRED);
            return;
        }
        chain.doFilter(request, response);
    }
}
