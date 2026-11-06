package com.tomanagement.tenant;

/**
 * Tenant ids that are not real establishments. {@link #NONE} is used when no establishment is in scope: it matches
 * no row, so reads come back empty, and it fails the foreign key on insert, so writes fail.
 */
public enum TenantSentinel {

    NONE(-1L);

    private final Long id;

    TenantSentinel(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
