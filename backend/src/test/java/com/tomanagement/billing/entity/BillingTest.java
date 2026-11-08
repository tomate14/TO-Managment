package com.tomanagement.billing.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.tomanagement.diagnosis.entity.ConservativeTreatment;
import com.tomanagement.diagnosis.entity.Diagnosis;
import com.tomanagement.followup.entity.FollowUp;
import com.tomanagement.patient.entity.Dominance;
import com.tomanagement.patient.entity.Patient;
import com.tomanagement.patient.entity.PatientData;

import org.junit.jupiter.api.Test;

class BillingTest {

    private static final Instant FIRST_VOID = Instant.parse("2026-11-08T12:00:00Z");
    private static final Instant SECOND_VOID = Instant.parse("2026-11-09T12:00:00Z");

    private final Diagnosis diagnosis = diagnosis();

    @Test
    void aNewBillingIsStandingWithActiveLinks() {
        Billing billing = billing();

        assertThat(billing.isVoided()).isFalse();
        assertThat(billing.getVoidedAt()).isNull();
        assertThat(billing.getLinks()).hasSize(2).allMatch(BillingFollowUp::isActive);
        assertThat(billing.getFollowUps()).hasSize(2);
    }

    @Test
    void voidingMarksTheTimeAndDeactivatesEveryLink() {
        Billing billing = billing();

        billing.voidBilling(FIRST_VOID);

        assertThat(billing.isVoided()).isTrue();
        assertThat(billing.getVoidedAt()).isEqualTo(FIRST_VOID);
        assertThat(billing.getLinks()).noneMatch(BillingFollowUp::isActive);
    }

    @Test
    void voidingAgainKeepsTheFirstVoidTime() {
        Billing billing = billing();
        billing.voidBilling(FIRST_VOID);

        billing.voidBilling(SECOND_VOID);

        assertThat(billing.getVoidedAt()).isEqualTo(FIRST_VOID);
        assertThat(billing.getLinks()).noneMatch(BillingFollowUp::isActive);
    }

    private Billing billing() {
        List<FollowUp> followUps = List.of(followUp("2026-11-01"), followUp("2026-11-03"));
        return new Billing(diagnosis, LocalDate.of(2026, 11, 8), new BigDecimal("25000.50"),
                Instant.parse("2026-11-08T10:00:00Z"), followUps);
    }

    private FollowUp followUp(String date) {
        return FollowUp.builder().diagnosis(diagnosis).date(LocalDate.parse(date)).build();
    }

    private static Diagnosis diagnosis() {
        Patient patient = Patient.builder()
                .dni("30123456")
                .data(new PatientData("Lucía", "Fernández", LocalDate.of(1984, 3, 21), "249 412-3456", "Docente",
                        "OSDE", "61 234567 8 01", Dominance.RIGHT))
                .createdAt(Instant.parse("2026-11-04T12:00:00Z"))
                .build();
        return new Diagnosis(patient, "Esguince", LocalDate.of(2026, 10, 12),
                new ConservativeTreatment(true, LocalDate.of(2026, 10, 1)));
    }
}
