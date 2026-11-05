package com.tomanagement.followup.validation;

import java.util.HashSet;
import java.util.Set;

import com.tomanagement.catalog.entity.BodySegment;
import com.tomanagement.catalog.repository.BodySegmentRepository;
import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.ResourceNotFoundException;
import com.tomanagement.followup.dto.SegmentEvaluationRequest;

import org.springframework.stereotype.Component;

/**
 * Every evaluated segment exists, is active, and appears only once in the follow-up.
 */
@Component
public class SegmentStep extends FollowUpValidationStep {

    private final BodySegmentRepository bodySegmentRepository;

    public SegmentStep(BodySegmentRepository bodySegmentRepository) {
        this.bodySegmentRepository = bodySegmentRepository;
    }

    @Override
    protected void check(FollowUpValidationContext context) {
        Set<Long> seen = new HashSet<>();
        for (SegmentEvaluationRequest evaluation : context.request().evaluations()) {
            if (!seen.add(evaluation.segmentId())) {
                throw new BusinessRuleException(ErrorCode.SEGMENT_REPEATED);
            }
            BodySegment segment = bodySegmentRepository.findById(evaluation.segmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CATALOG_ITEM_NOT_FOUND));
            if (!segment.isActive()) {
                throw new BusinessRuleException(ErrorCode.SEGMENT_INACTIVE);
            }
        }
    }
}
