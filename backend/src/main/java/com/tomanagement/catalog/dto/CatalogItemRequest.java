package com.tomanagement.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Name of a new or renamed catalog item; trimmed before validation.
 */
public record CatalogItemRequest(
        @NotBlank(message = "Ingresá un nombre.") @Size(max = 80, message = "El nombre es demasiado largo.")
        String name) {

    public CatalogItemRequest {
        name = name == null ? null : name.trim();
    }
}
