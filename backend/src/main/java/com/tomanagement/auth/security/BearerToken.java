package com.tomanagement.auth.security;

import java.util.Optional;

/**
 * Extracts the token from an "Authorization: Bearer <token>" header value.
 */
public enum BearerToken {

    PREFIX("Bearer ");

    private final String value;

    BearerToken(String value) {
        this.value = value;
    }

    public static Optional<String> extract(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(PREFIX.value)) {
            return Optional.empty();
        }
        return Optional.of(authorizationHeader.substring(PREFIX.value.length()).trim());
    }
}
