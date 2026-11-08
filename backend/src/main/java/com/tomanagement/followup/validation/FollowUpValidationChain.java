package com.tomanagement.followup.validation;

import org.springframework.stereotype.Component;

/**
 * Chain of Responsibility (spec §4): treatment → clinical ranges → segments → segment metrics →
 * template answers. The first failing
 * step stops the chain.
 */
@Component
public class FollowUpValidationChain {

    private final FollowUpValidationStep first;

    public FollowUpValidationChain(TreatmentScarTypeStep treatment, ClinicalRangeStep clinicalRanges,
            SegmentStep segments, SegmentMetricsStep metrics, TemplateAnswersStep templateAnswers) {
        treatment.then(clinicalRanges).then(segments).then(metrics).then(templateAnswers);
        this.first = treatment;
    }

    public void validate(FollowUpValidationContext context) {
        first.validate(context);
    }
}
