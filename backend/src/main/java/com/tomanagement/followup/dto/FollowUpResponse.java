package com.tomanagement.followup.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.tomanagement.catalog.dto.CatalogItemResponse;
import com.tomanagement.template.entity.AnswerType;

/**
 * A follow-up in the history (H6.4).
 *
 * @param scarType null for conservative treatments
 */
public record FollowUpResponse(
        Long id,
        Long diagnosisId,
        LocalDate date,
        CatalogItemResponse scarType,
        List<SegmentEvaluationResponse> evaluations) {

    /**
     * @param edema    null when it was omitted
     * @param template null when no template was answered
     * @param answers  in the template tree order
     */
    public record SegmentEvaluationResponse(
            Long id,
            CatalogItemResponse segment,
            BigDecimal edema,
            int pain,
            BigDecimal rom,
            BigDecimal strength,
            TemplateRef template,
            List<AnswerResponse> answers) {
    }

    public record TemplateRef(Long id, String name) {
    }

    /**
     * An answer with the question text, even when the question is now inactive.
     *
     * @param depth 0 for top-level questions, +1 per chaining level
     */
    public record AnswerResponse(
            Long questionId,
            String questionText,
            int depth,
            AnswerType answerType,
            BigDecimal numericValue,
            Boolean booleanValue,
            String optionValue,
            String textValue) {

        @Override
        public String toString() {
            return "AnswerResponse[questionId=" + questionId + ", ***]";
        }
    }
}
