package com.tomanagement.tenant;

import java.util.Optional;

import com.tomanagement.common.exception.AccessDeniedApplicationException;

/**
 * Adapter over the security context for services that need the session's establishment explicitly (users, agenda).
 */
public interface CurrentEstablishmentProvider {

    Optional<Long> establishmentId();

    /**
     * @throws AccessDeniedApplicationException with {@code ESTABLISHMENT_REQUIRED} when the session has none
     */
    Long requireEstablishmentId();
}
