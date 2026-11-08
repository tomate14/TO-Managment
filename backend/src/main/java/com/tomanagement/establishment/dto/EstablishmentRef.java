package com.tomanagement.establishment.dto;

import com.tomanagement.establishment.entity.Establishment;
import com.tomanagement.establishment.entity.EstablishmentKind;

/** Minimal reference to an establishment, embedded in the session summary. */
public record EstablishmentRef(Long id, String name, EstablishmentKind kind) {

    public static EstablishmentRef from(Establishment establishment) {
        return new EstablishmentRef(establishment.getId(), establishment.getName(), establishment.getKind());
    }
}
