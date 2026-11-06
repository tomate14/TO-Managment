package com.tomanagement.auth.dto;

/** Establishment the superadmin wants to work in; null means leaving the current one. */
public record ChangeEstablishmentRequest(Long establishmentId) {
}
