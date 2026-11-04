package com.tomanagement.catalog.service;

import java.util.List;

import com.tomanagement.catalog.dto.CatalogItemResponse;

/**
 * Maintenance of one catalog (H2.1–H2.3).
 */
public interface CatalogService {

    CatalogType type();

    /**
     * @param activeOnly true for pickers in new records; false for the admin screen
     */
    List<CatalogItemResponse> list(boolean activeOnly);

    CatalogItemResponse create(String name);

    CatalogItemResponse rename(Long id, String name);

    CatalogItemResponse changeActive(Long id, boolean active);
}
