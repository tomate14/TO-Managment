package com.tomanagement.establishment.dto;

import java.time.Instant;
import java.util.List;

import com.tomanagement.establishment.entity.EstablishmentKind;

public record EstablishmentSummaryResponse(Long id, String name, EstablishmentKind kind, boolean active,
        boolean pendingApproval, boolean rejected, Instant createdAt,
        Instant trialEndsAt, boolean trialExpired, long activeUsers, List<AdminRef> admins) {

    public record AdminRef(Long id, String fullName, String email) {
    }
}
