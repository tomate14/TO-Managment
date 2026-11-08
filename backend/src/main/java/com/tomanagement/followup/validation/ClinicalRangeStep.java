package com.tomanagement.followup.validation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.FieldValidationException;
import com.tomanagement.common.exception.dto.FieldErrorResponse;
import com.tomanagement.followup.dto.SegmentEvaluationRequest;

import org.springframework.stereotype.Component;

/**
 * Checks every metric against its {@link ClinicalRange} (H6.2) and reports all the failures at once, one per field
 * ({@code evaluations[i].strength}).
 */
@Component
public class ClinicalRangeStep extends FollowUpValidationStep {

    @Override
    protected void check(FollowUpValidationContext context) {
        List<SegmentEvaluationRequest> evaluations = context.request().evaluations();
        List<FieldErrorResponse> errors = new ArrayList<>();
        for (int index = 0; index < evaluations.size(); index++) {
            SegmentEvaluationRequest evaluation = evaluations.get(index);
            if (evaluation.edema() != null) {
                checkRange(index, ClinicalRange.EDEMA, evaluation.edema(), errors);
            }
            checkRange(index, ClinicalRange.PAIN, BigDecimal.valueOf(evaluation.pain()), errors);
            checkRange(index, ClinicalRange.STRENGTH, evaluation.strength(), errors);
        }
        if (!errors.isEmpty()) {
            throw new FieldValidationException(ErrorCode.FOLLOW_UP_OUT_OF_RANGE, errors);
        }
    }

    private static void checkRange(int index, ClinicalRange range, BigDecimal value, List<FieldErrorResponse> errors) {
        if (!range.contains(value)) {
            errors.add(new FieldErrorResponse("evaluations[" + index + "]." + range.getFieldName(),
                    range.outOfRangeMessage()));
        }
    }
}
