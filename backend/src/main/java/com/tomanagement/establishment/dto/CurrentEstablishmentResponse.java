package com.tomanagement.establishment.dto;

import java.time.LocalDate;

public record CurrentEstablishmentResponse(Long id, String name, long activeUsers, LocalDate createdDate) {
}
