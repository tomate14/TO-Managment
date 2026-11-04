package com.tomanagement.template.dto;

/**
 * A template card in the list: counts include inactive questions.
 */
public record TemplateSummaryResponse(
        Long id, String name, SegmentRef segment, boolean active, int questionCount, int chainedCount) {
}
