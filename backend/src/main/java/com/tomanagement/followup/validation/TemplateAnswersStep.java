package com.tomanagement.followup.validation;

import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.followup.dto.SegmentEvaluationRequest;
import com.tomanagement.followup.service.TemplateAnswerReader;

import org.springframework.stereotype.Component;

/**
 * Template answers (H6.3): without a template there are no answers; with one, the reader checks the template and
 * every answer.
 */
@Component
public class TemplateAnswersStep extends FollowUpValidationStep {

    private final TemplateAnswerReader answerReader;

    public TemplateAnswersStep(TemplateAnswerReader answerReader) {
        this.answerReader = answerReader;
    }

    @Override
    protected void check(FollowUpValidationContext context) {
        for (SegmentEvaluationRequest evaluation : context.request().evaluations()) {
            if (evaluation.templateId() == null) {
                if (!evaluation.answersOrEmpty().isEmpty()) {
                    throw new BusinessRuleException(ErrorCode.ANSWER_QUESTION_INVALID);
                }
            } else {
                answerReader.read(evaluation.segmentId(), evaluation.templateId(), evaluation.answersOrEmpty());
            }
        }
    }
}
