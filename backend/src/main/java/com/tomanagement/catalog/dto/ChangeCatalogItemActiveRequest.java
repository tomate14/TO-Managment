package com.tomanagement.catalog.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeCatalogItemActiveRequest(@NotNull(message = "Indicá si queda activo.") Boolean active) {
}
