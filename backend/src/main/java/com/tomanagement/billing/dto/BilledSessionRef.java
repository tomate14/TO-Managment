package com.tomanagement.billing.dto;

import java.time.LocalDate;
import java.util.List;

import com.tomanagement.followup.entity.FollowUp;
import com.tomanagement.followup.entity.SegmentEvaluation;

/**
 * A follow-up as a billable session: its date and the names of the segments evaluated, in evaluation order.
 */
public record BilledSessionRef(Long followUpId, LocalDate date, List<String> segments) {

    public static BilledSessionRef from(FollowUp followUp) {
        return new BilledSessionRef(followUp.getId(), followUp.getDate(),
                followUp.getEvaluations().stream().map(SegmentEvaluation::getSegment)
                        .map(segment -> segment.getName()).toList());
    }
}
