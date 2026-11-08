package com.tomanagement.segmentmetric.dto;

import java.util.List;

import com.tomanagement.segmentmetric.entity.SegmentMetric;
import com.tomanagement.template.entity.AnswerType;

public record SegmentMetricResponse(Long id, Long segmentId, String name, AnswerType answerType, boolean required,
        boolean active, int displayOrder, List<String> options) {

    public static SegmentMetricResponse from(SegmentMetric metric) {
        return new SegmentMetricResponse(metric.getId(), metric.getSegment().getId(), metric.getName(),
                metric.getAnswerType(), metric.isRequired(), metric.isActive(), metric.getDisplayOrder(),
                metric.getOptionLabels());
    }
}
