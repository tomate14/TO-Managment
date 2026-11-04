package com.tomanagement.catalog.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "body_segment")
public class BodySegment extends CatalogItem {

    protected BodySegment() {
        // Required by JPA.
    }

    public BodySegment(String name) {
        super(name);
    }
}
