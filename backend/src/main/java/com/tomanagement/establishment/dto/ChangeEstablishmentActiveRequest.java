package com.tomanagement.establishment.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeEstablishmentActiveRequest(@NotNull(message = "Indicá si el establecimiento está activo.") Boolean active) {
}
