package com.tomanagement.catalog.dto;

import com.tomanagement.catalog.entity.CatalogItem;

public record CatalogItemResponse(Long id, String name, boolean active) {

    public static CatalogItemResponse from(CatalogItem item) {
        return new CatalogItemResponse(item.getId(), item.getName(), item.isActive());
    }
}
