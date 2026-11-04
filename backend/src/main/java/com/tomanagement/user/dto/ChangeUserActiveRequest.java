package com.tomanagement.user.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeUserActiveRequest(@NotNull(message = "Indicá si el usuario queda activo.") Boolean active) {
}
