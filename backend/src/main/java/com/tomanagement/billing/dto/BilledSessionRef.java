package com.tomanagement.billing.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.tomanagement.appointment.entity.Appointment;
import com.tomanagement.billing.entity.BillingSessionOrigin;
import com.tomanagement.diagnosis.entity.Diagnosis;
import com.tomanagement.followup.entity.FollowUp;
import com.tomanagement.followup.entity.SegmentEvaluation;

public record BilledSessionRef(BillingSessionOrigin origin, Long appointmentId, Long followUpId,
        LocalDate date, LocalTime startTime, Instant confirmedAt, DiagnosisRef diagnosis, List<String> segments) {

    public record DiagnosisRef(Long id, String description) {

        static DiagnosisRef from(Diagnosis diagnosis) {
            return new DiagnosisRef(diagnosis.getId(), diagnosis.getDescription());
        }
    }

    public static BilledSessionRef from(Appointment appointment) {
        return new BilledSessionRef(BillingSessionOrigin.APPOINTMENT, appointment.getId(), null,
                appointment.getStartAt().toLocalDate(), appointment.getStartAt().toLocalTime(),
                appointment.getConfirmedAt(), appointment.getDiagnosis().map(DiagnosisRef::from).orElse(null),
                List.of());
    }

    public static BilledSessionRef from(FollowUp followUp) {
        return new BilledSessionRef(BillingSessionOrigin.LEGACY_FOLLOW_UP, null, followUp.getId(),
                followUp.getDate(), null, null, DiagnosisRef.from(followUp.getDiagnosis()),
                followUp.getEvaluations().stream().map(SegmentEvaluation::getSegment)
                        .map(segment -> segment.getName()).toList());
    }
}
