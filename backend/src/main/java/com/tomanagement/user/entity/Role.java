package com.tomanagement.user.entity;

/**
 * Roles of the system users (spec section 2). Only the platform creates the superadmin; establishment admins can
 * assign the other roles.
 */
public enum Role {
    SUPERADMIN(false),
    ADMIN(true),
    TERAPEUTA(true);

    private final boolean assignableInEstablishment;

    Role(boolean assignableInEstablishment) {
        this.assignableInEstablishment = assignableInEstablishment;
    }

    public boolean isAssignableInEstablishment() {
        return assignableInEstablishment;
    }
}
