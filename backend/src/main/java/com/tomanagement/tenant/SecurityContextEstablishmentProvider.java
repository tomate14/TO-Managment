package com.tomanagement.tenant;

import java.util.Optional;

import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.common.exception.AccessDeniedApplicationException;
import com.tomanagement.common.exception.ErrorCode;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Adapter pattern: reads the establishment of the {@link AuthenticatedUser} principal put in the security context by
 * the JWT filter.
 */
@Component
public class SecurityContextEstablishmentProvider implements CurrentEstablishmentProvider {

    @Override
    public Optional<Long> establishmentId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.ofNullable(user.establishmentId());
        }
        return Optional.empty();
    }

    @Override
    public Long requireEstablishmentId() {
        return establishmentId()
                .orElseThrow(() -> new AccessDeniedApplicationException(ErrorCode.ESTABLISHMENT_REQUIRED));
    }
}
