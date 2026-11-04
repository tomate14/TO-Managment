package com.tomanagement.auth.security;

/**
 * Names of the custom claims written into the JWT.
 */
public enum TokenClaim {

    EMAIL("email"),
    FULL_NAME("name"),
    ROLE("role");

    private final String claimName;

    TokenClaim(String claimName) {
        this.claimName = claimName;
    }

    public String getClaimName() {
        return claimName;
    }
}
