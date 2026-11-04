package com.tomanagement.template.service;

import com.tomanagement.template.dto.QuestionRequest;
import com.tomanagement.template.dto.TemplateDetailResponse;

/**
 * Questions of a template (H3.2–H3.4). Every change returns the refreshed template so the editor can redraw.
 */
public interface QuestionService {

    TemplateDetailResponse create(Long templateId, QuestionRequest request);

    /**
     * Rejected for questions with answers; for questions with dependents, the type and options cannot change.
     */
    TemplateDetailResponse update(Long templateId, Long questionId, QuestionRequest request);

    /**
     * Always allowed, also for questions with answers.
     */
    TemplateDetailResponse changeActive(Long templateId, Long questionId, boolean active);

    /**
     * Rejected for questions with answers or dependents.
     */
    void delete(Long templateId, Long questionId);
}
