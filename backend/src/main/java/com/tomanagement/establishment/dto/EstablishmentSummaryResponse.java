package com.tomanagement.establishment.dto;

import java.util.List;

public record EstablishmentSummaryResponse(Long id, String name, boolean active, long activeUsers,
        List<AdminRef> admins) {

    public record AdminRef(Long id, String fullName, String email) {
    }
}
