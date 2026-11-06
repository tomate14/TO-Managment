package com.tomanagement.tenant;

import java.util.List;

/**
 * URL prefixes whose data belongs to an establishment: requests to them need an establishment in the session.
 */
public enum TenantScopedPath {

    PATIENTS("/api/patients"),
    DIAGNOSES("/api/diagnoses"),
    CATALOGS("/api/catalogs"),
    TEMPLATES("/api/templates"),
    APPOINTMENTS("/api/appointments"),
    AGENDA("/api/agenda"),
    USERS("/api/users"),
    CURRENT_ESTABLISHMENT("/api/establishment");

    private static final String ANY_SUBPATH = "/**";

    private final String prefix;

    TenantScopedPath(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    /**
     * The bare prefix and everything below it.
     */
    public List<String> getPatterns() {
        return List.of(prefix, prefix + ANY_SUBPATH);
    }
}
