package com.tomanagement.catalog.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "scar_type")
public class ScarType extends CatalogItem {

    protected ScarType() {
        // Required by JPA.
    }

    public ScarType(String name) {
        super(name);
    }
}
