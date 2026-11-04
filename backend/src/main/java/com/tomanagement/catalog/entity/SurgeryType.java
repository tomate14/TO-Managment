package com.tomanagement.catalog.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "surgery_type")
public class SurgeryType extends CatalogItem {

    protected SurgeryType() {
        // Required by JPA.
    }

    public SurgeryType(String name) {
        super(name);
    }
}
