package com.tomanagement.followup.validation;

import java.util.List;

import com.tomanagement.followup.dto.SegmentEvaluationRequest;
import com.tomanagement.followup.service.SegmentMetricValueReader;

import org.springframework.stereotype.Component;

/**
 * Segment ROM metric values (É11): the reader checks the values of every evaluation against the active metrics of its
 * segment.
 */
@Component
public class SegmentMetricsStep extends FollowUpValidationStep {

    private final SegmentMetricValueReader metricValueReader;

    public SegmentMetricsStep(SegmentMetricValueReader metricValueReader) {
        this.metricValueReader = metricValueReader;
    }

    @Override
    protected void check(FollowUpValidationContext context) {
        List<SegmentEvaluationRequest> evaluations = context.request().evaluations();
        for (int index = 0; index < evaluations.size(); index++) {
            SegmentEvaluationRequest evaluation = evaluations.get(index);
            metricValueReader.read(index, evaluation.segmentId(), evaluation.metricsOrEmpty());
        }
    }
}
