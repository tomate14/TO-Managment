package com.tomanagement.establishment.dto;

import com.tomanagement.establishment.entity.Establishment;

/** Minimal reference to an establishment, embedded in the session summary. */
public record EstablishmentRef(Long id, String name) {

    public static EstablishmentRef from(Establishment establishment) {
        return new EstablishmentRef(establishment.getId(), establishment.getName());
    }
}
