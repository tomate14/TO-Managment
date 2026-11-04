package com.tomanagement.template.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeActiveRequest(@NotNull(message = "Indicá si queda activa.") Boolean active) {
}
