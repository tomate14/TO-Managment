package com.tomanagement.segmentmetric.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeSegmentMetricActiveRequest(@NotNull Boolean active) {
}
