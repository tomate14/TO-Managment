package com.tomanagement.template.dto;

import java.util.List;

/**
 * A template with its questions in tree order (each child right after its parent).
 */
public record TemplateDetailResponse(
        Long id, String name, SegmentRef segment, boolean active, List<QuestionResponse> questions) {
}
