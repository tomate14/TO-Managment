package com.tomanagement.establishment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameEstablishmentRequest(
        @NotBlank(message = "Ingresá el nombre del establecimiento.")
        @Size(max = 120, message = "El nombre es demasiado largo.") String name) {

    public RenameEstablishmentRequest {
        name = name == null ? null : name.trim();
    }
}
