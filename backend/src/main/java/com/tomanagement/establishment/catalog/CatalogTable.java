package com.tomanagement.establishment.catalog;

import java.util.Arrays;

import com.tomanagement.catalog.service.CatalogType;

/**
 * Catalog tables the seeder writes to (the JPA catalog entities map to these tables).
 */
public enum CatalogTable {
    BODY_SEGMENTS(CatalogType.BODY_SEGMENTS, "body_segment"),
    SURGERY_TYPES(CatalogType.SURGERY_TYPES, "surgery_type"),
    SCAR_TYPES(CatalogType.SCAR_TYPES, "scar_type");

    private final CatalogType type;
    private final String tableName;

    CatalogTable(CatalogType type, String tableName) {
        this.type = type;
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    public static CatalogTable of(CatalogType type) {
        return Arrays.stream(values())
                .filter(table -> table.type == type)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No table mapped for catalog " + type));
    }
}
