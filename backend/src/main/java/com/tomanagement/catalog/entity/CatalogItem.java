package com.tomanagement.catalog.entity;

import java.util.Objects;

import org.hibernate.annotations.TenantId;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * Common shape of every admin-maintained catalog (name unique ignoring case, deactivated instead of deleted).
 */
@MappedSuperclass
public abstract class CatalogItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "establishment_id", nullable = false, updatable = false)
    private Long establishmentId;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false)
    private boolean active;

    protected CatalogItem() {
        // Required by JPA.
    }

    protected CatalogItem(String name) {
        this.name = Objects.requireNonNull(name, "name is required");
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    public void rename(String name) {
        this.name = Objects.requireNonNull(name, "name is required");
    }

    public void changeActive(boolean active) {
        this.active = active;
    }
}
