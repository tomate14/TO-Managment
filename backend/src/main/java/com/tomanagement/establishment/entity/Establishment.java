package com.tomanagement.establishment.entity;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A tenant: a clinic or practice whose users and data are isolated from every other establishment (É8).
 */
@Entity
@Table(name = "establishment")
public class Establishment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstablishmentKind kind;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Establishment() {
        // Required by JPA.
    }

    public Establishment(String name, EstablishmentKind kind, Instant createdAt) {
        this.name = Objects.requireNonNull(name, "name is required");
        this.kind = Objects.requireNonNull(kind, "kind is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public EstablishmentKind getKind() {
        return kind;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void rename(String name) {
        this.name = Objects.requireNonNull(name, "name is required");
    }

    public void changeActive(boolean active) {
        this.active = active;
    }

    /**
     * Approves the establishment. The first approval records the date and starts the trial; approving again keeps
     * both dates (and an expiry the superadmin removed stays removed). Approval always (re)activates.
     */
    public void approve(Instant when, Duration trial) {
        Objects.requireNonNull(when, "when is required");
        Objects.requireNonNull(trial, "trial is required");
        if (approvedAt == null) {
            approvedAt = when;
            trialEndsAt = when.plus(trial);
        }
        active = true;
    }

    /** Sets a new trial end; {@code null} removes the expiry (paid account). */
    public void changeTrialEnd(Instant endsAt) {
        this.trialEndsAt = endsAt;
    }

    public boolean isTrialExpired(Instant now) {
        return trialEndsAt != null && !now.isBefore(trialEndsAt);
    }

    public Instant getTrialEndsAt() {
        return trialEndsAt;
    }

    public boolean isPendingApproval() {
        return approvedAt == null && active;
    }

    public boolean isRejected() {
        return approvedAt == null && !active;
    }
}
