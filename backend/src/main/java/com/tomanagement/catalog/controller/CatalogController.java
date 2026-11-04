package com.tomanagement.catalog.controller;

import java.util.List;

import com.tomanagement.catalog.dto.CatalogItemRequest;
import com.tomanagement.catalog.dto.CatalogItemResponse;
import com.tomanagement.catalog.dto.ChangeCatalogItemActiveRequest;
import com.tomanagement.catalog.service.CatalogServiceRegistry;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catalogs: every signed-in user can read them (pickers); only admins change them.
 */
@RestController
@RequestMapping("/api/catalogs/{slug}")
public class CatalogController {

    private final CatalogServiceRegistry registry;

    public CatalogController(CatalogServiceRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public List<CatalogItemResponse> list(@PathVariable("slug") String slug,
            @RequestParam(name = "activeOnly", defaultValue = "false") boolean activeOnly) {
        return registry.forSlug(slug).list(activeOnly);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CatalogItemResponse create(@PathVariable("slug") String slug,
            @Valid @RequestBody CatalogItemRequest request) {
        return registry.forSlug(slug).create(request.name());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CatalogItemResponse rename(@PathVariable("slug") String slug, @PathVariable("id") Long id,
            @Valid @RequestBody CatalogItemRequest request) {
        return registry.forSlug(slug).rename(id, request.name());
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public CatalogItemResponse changeActive(@PathVariable("slug") String slug, @PathVariable("id") Long id,
            @Valid @RequestBody ChangeCatalogItemActiveRequest request) {
        return registry.forSlug(slug).changeActive(id, request.active());
    }
}
