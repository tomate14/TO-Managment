package com.tomanagement.catalog.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.ResourceNotFoundException;

import org.springframework.stereotype.Component;

/**
 * Strategy lookup: picks the catalog service for a URL slug. Every {@link CatalogService} bean registers itself
 * by its {@link CatalogType}, so a new catalog only needs a new service.
 */
@Component
public class CatalogServiceRegistry {

    private final Map<CatalogType, CatalogService> services = new EnumMap<>(CatalogType.class);

    public CatalogServiceRegistry(List<CatalogService> catalogServices) {
        catalogServices.forEach(service -> services.put(service.type(), service));
    }

    public CatalogService forSlug(String slug) {
        return CatalogType.fromSlug(slug)
                .map(services::get)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CATALOG_NOT_FOUND));
    }
}
