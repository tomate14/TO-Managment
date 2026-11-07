package com.tomanagement.establishment.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class EstablishmentTest {

    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");

    @Test
    void anIndividualPracticeIsBornPending() {
        Establishment e = new Establishment("Ana Pérez", EstablishmentKind.INDIVIDUAL, NOW);
        assertThat(e.isPendingApproval()).isTrue();
        assertThat(e.isRejected()).isFalse();
    }

    @Test
    void approvingSetsTheDateAndReactivates() {
        Establishment e = new Establishment("Ana Pérez", EstablishmentKind.INDIVIDUAL, NOW);
        e.changeActive(false);
        assertThat(e.isRejected()).isTrue();
        e.approve(NOW.plusSeconds(60));
        assertThat(e.getApprovedAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(e.isActive()).isTrue();
        assertThat(e.isPendingApproval()).isFalse();
    }

    @Test
    void approvingTwiceKeepsTheFirstDate() {
        Establishment e = new Establishment("Ana Pérez", EstablishmentKind.INDIVIDUAL, NOW);
        e.approve(NOW);
        e.approve(NOW.plusSeconds(60));
        assertThat(e.getApprovedAt()).isEqualTo(NOW);
    }
}
