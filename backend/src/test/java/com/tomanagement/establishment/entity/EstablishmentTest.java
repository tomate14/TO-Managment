package com.tomanagement.establishment.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class EstablishmentTest {

    private static final Duration TRIAL = Duration.ofDays(7);
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
        e.approve(NOW.plusSeconds(60), TRIAL);
        assertThat(e.getApprovedAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(e.isActive()).isTrue();
        assertThat(e.isPendingApproval()).isFalse();
    }

    @Test
    void approvingTwiceKeepsTheFirstDateAndTheFirstExpiry() {
        Establishment e = new Establishment("Ana Pérez", EstablishmentKind.INDIVIDUAL, NOW);
        e.approve(NOW, TRIAL);
        e.approve(NOW.plus(Duration.ofDays(3)), TRIAL);
        assertThat(e.getApprovedAt()).isEqualTo(NOW);
        assertThat(e.getTrialEndsAt()).isEqualTo(NOW.plus(TRIAL));
    }

    @Test
    void theFirstApprovalStartsTheTrial() {
        Establishment e = new Establishment("Ana Pérez", EstablishmentKind.INDIVIDUAL, NOW);
        assertThat(e.getTrialEndsAt()).isNull();
        e.approve(NOW, TRIAL);
        assertThat(e.getTrialEndsAt()).isEqualTo(NOW.plus(TRIAL));
    }

    @Test
    void reApprovingAfterRemovingTheExpiryKeepsItRemoved() {
        Establishment e = new Establishment("Centro", EstablishmentKind.INSTITUTION, NOW);
        e.approve(NOW, TRIAL);
        e.changeTrialEnd(null);
        e.approve(NOW.plusSeconds(60), TRIAL);
        assertThat(e.getTrialEndsAt()).isNull();
    }

    @Test
    void theTrialExpiresExactlyAtItsEnd() {
        Establishment e = new Establishment("Centro", EstablishmentKind.INSTITUTION, NOW);
        e.approve(NOW, TRIAL);
        Instant end = NOW.plus(TRIAL);
        assertThat(e.isTrialExpired(end.minusSeconds(1))).isFalse();
        assertThat(e.isTrialExpired(end)).isTrue();
    }

    @Test
    void withoutExpiryItNeverExpires() {
        Establishment e = new Establishment("Centro", EstablishmentKind.INSTITUTION, NOW);
        e.approve(NOW, TRIAL);
        e.changeTrialEnd(null);
        assertThat(e.isTrialExpired(NOW.plus(Duration.ofDays(3650)))).isFalse();
    }

    @Test
    void changingTheEndMovesIt() {
        Establishment e = new Establishment("Centro", EstablishmentKind.INSTITUTION, NOW);
        e.approve(NOW, TRIAL);
        e.changeTrialEnd(NOW.plus(Duration.ofDays(30)));
        assertThat(e.getTrialEndsAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
    }
}
