package com.tomanagement.establishment.catalog;

import com.tomanagement.catalog.service.CatalogType;

/**
 * Catalog rows copied into every new establishment (spec §2).
 */
public enum DefaultCatalogEntry {
    SEGMENT_SHOULDER(CatalogType.BODY_SEGMENTS, "Hombro"),
    SEGMENT_ELBOW(CatalogType.BODY_SEGMENTS, "Codo"),
    SEGMENT_WRIST(CatalogType.BODY_SEGMENTS, "Muñeca"),
    SEGMENT_HAND(CatalogType.BODY_SEGMENTS, "Mano"),
    SEGMENT_FINGERS(CatalogType.BODY_SEGMENTS, "Dedos"),
    SURGERY_PLATE(CatalogType.SURGERY_TYPES, "Osteosíntesis con placa"),
    SURGERY_TENORRHAPHY(CatalogType.SURGERY_TYPES, "Tenorrafia"),
    SURGERY_CARPAL_TUNNEL(CatalogType.SURGERY_TYPES, "Liberación del túnel carpiano"),
    SURGERY_ARTHRODESIS(CatalogType.SURGERY_TYPES, "Artrodesis"),
    SCAR_NORMOTROPHIC(CatalogType.SCAR_TYPES, "Normotrófica"),
    SCAR_HYPERTROPHIC(CatalogType.SCAR_TYPES, "Hipertrófica"),
    SCAR_KELOID(CatalogType.SCAR_TYPES, "Queloide"),
    SCAR_RETRACTILE(CatalogType.SCAR_TYPES, "Retráctil");

    private final CatalogType type;
    private final String name;

    DefaultCatalogEntry(CatalogType type, String name) {
        this.type = type;
        this.name = name;
    }

    public CatalogType getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
