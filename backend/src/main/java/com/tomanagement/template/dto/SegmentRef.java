package com.tomanagement.template.dto;

import com.tomanagement.catalog.entity.BodySegment;

public record SegmentRef(Long id, String name, boolean active) {

    public static SegmentRef from(BodySegment segment) {
        return new SegmentRef(segment.getId(), segment.getName(), segment.isActive());
    }
}
