package com.tomanagement.segmentmetric.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record SegmentMetricOrderRequest(@NotEmpty List<@NotNull Long> ids) {
}
