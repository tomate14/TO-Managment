package com.tomanagement.user.entity;

/**
 * Roles of the system users (spec section 2). Only the platform creates the superadmin; establishment admins can
 * assign the other roles.
 */
public enum Role {
    SUPERADMIN(false, false),
    ADMIN(true, true),
    TERAPEUTA(true, true);

    private final boolean assignableInEstablishment;
    private final boolean requiresEstablishment;

    Role(boolean assignableInEstablishment, boolean requiresEstablishment) {
        this.assignableInEstablishment = assignableInEstablishment;
        this.requiresEstablishment = requiresEstablishment;
    }

    /** Whether users with this role always belong to an establishment (only the superadmin does not). */
    public boolean requiresEstablishment() {
        return requiresEstablishment;
    }

    public boolean isAssignableInEstablishment() {
        return assignableInEstablishment;
    }
}
