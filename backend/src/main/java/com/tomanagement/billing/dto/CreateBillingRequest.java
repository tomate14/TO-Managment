package com.tomanagement.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record CreateBillingRequest(
        @NotNull(message = "Ingresá la fecha de facturación.") LocalDate billedOn,
        @NotNull(message = "Ingresá el monto.")
        @Digits(integer = 10, fraction = 2, message = "El monto no es válido.") BigDecimal amount,
        List<@NotNull Long> followUpIds) {

    public List<Long> followUpIdsOrEmpty() {
        return followUpIds == null ? List.of() : followUpIds;
    }

    /** The amount never reaches logs. */
    @Override
    public String toString() {
        return "CreateBillingRequest[billedOn=" + billedOn + ", ***]";
    }
}
