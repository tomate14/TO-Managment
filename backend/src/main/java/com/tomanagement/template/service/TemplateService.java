package com.tomanagement.template.service;

import java.util.List;

import com.tomanagement.template.dto.TemplateDetailResponse;
import com.tomanagement.template.dto.TemplateRequest;
import com.tomanagement.template.dto.TemplateSummaryResponse;

/**
 * Evaluation templates (H3.1, H3.4).
 */
public interface TemplateService {

    /**
     * @param segmentId optional filter; null lists every template (active and inactive)
     */
    List<TemplateSummaryResponse> list(Long segmentId);

    TemplateDetailResponse get(Long templateId);

    /**
     * Requires an active segment; the name is unique within the segment (ignoring case).
     */
    TemplateDetailResponse create(TemplateRequest request);

    /**
     * Same rules as create; keeping the current segment is allowed even if it was deactivated later.
     */
    TemplateDetailResponse update(Long templateId, TemplateRequest request);

    TemplateDetailResponse changeActive(Long templateId, boolean active);
}
