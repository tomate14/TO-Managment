package com.tomanagement.establishment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * New establishment with its first admin.
 */
public record CreateEstablishmentRequest(
        @NotBlank(message = "Ingresá el nombre del establecimiento.")
        @Size(max = 120, message = "El nombre es demasiado largo.") String name,
        @NotNull(message = "Ingresá los datos del administrador.") @Valid EstablishmentAdminRequest admin) {

    public CreateEstablishmentRequest {
        name = name == null ? null : name.trim();
    }
}
