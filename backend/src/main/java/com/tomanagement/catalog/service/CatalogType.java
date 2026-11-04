package com.tomanagement.catalog.service;

import java.util.Arrays;
import java.util.Optional;

/**
 * The catalogs exposed under /api/catalogs/{slug}.
 */
public enum CatalogType {
    SURGERY_TYPES("surgery-types"),
    SCAR_TYPES("scar-types"),
    BODY_SEGMENTS("body-segments");

    private final String slug;

    CatalogType(String slug) {
        this.slug = slug;
    }

    public String getSlug() {
        return slug;
    }

    public static Optional<CatalogType> fromSlug(String slug) {
        return Arrays.stream(values()).filter(type -> type.slug.equals(slug)).findFirst();
    }
}
