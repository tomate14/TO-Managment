package com.tomanagement.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Strategy pattern (Hibernate's tenant resolution strategy): the {@link TenantContext} override wins, else the JWT
 * principal's establishment, else {@link TenantSentinel#NONE} so nothing is read and nothing can be written.
 * Consulted every time Hibernate opens a session (open-in-view is off: once per transaction or repository call).
 */
@Component
public class AuthenticatedTenantResolver implements CurrentTenantIdentifierResolver<Long> {

    private final CurrentEstablishmentProvider currentEstablishmentProvider;

    public AuthenticatedTenantResolver(CurrentEstablishmentProvider currentEstablishmentProvider) {
        this.currentEstablishmentProvider = currentEstablishmentProvider;
    }

    @Override
    public Long resolveCurrentTenantIdentifier() {
        return TenantContext.current()
                .or(currentEstablishmentProvider::establishmentId)
                .orElse(TenantSentinel.NONE.getId());
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
