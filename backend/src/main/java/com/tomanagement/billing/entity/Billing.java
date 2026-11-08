package com.tomanagement.billing.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.tomanagement.diagnosis.entity.Diagnosis;
import com.tomanagement.followup.entity.FollowUp;

import org.hibernate.annotations.TenantId;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * A billing of a group of follow-up sessions of a diagnosis (É12). It is never edited: a mistake is voided and
 * registered again.
 */
@Entity
@Table(name = "billing")
public class Billing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "establishment_id", nullable = false, updatable = false)
    private Long establishmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diagnosis_id", nullable = false, updatable = false)
    private Diagnosis diagnosis;

    @Column(name = "billed_on", nullable = false)
    private LocalDate billedOn;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @OneToMany(mappedBy = "billing", cascade = CascadeType.ALL)
    private List<BillingFollowUp> links = new ArrayList<>();

    protected Billing() {
        // Required by JPA.
    }

    public Billing(Diagnosis diagnosis, LocalDate billedOn, BigDecimal amount, Instant createdAt,
            List<FollowUp> followUps) {
        this.diagnosis = Objects.requireNonNull(diagnosis, "diagnosis is required");
        this.billedOn = Objects.requireNonNull(billedOn, "billedOn is required");
        this.amount = Objects.requireNonNull(amount, "amount is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        Objects.requireNonNull(followUps, "followUps is required")
                .forEach(followUp -> links.add(new BillingFollowUp(this, followUp)));
    }

    /**
     * Voids the billing and frees its sessions. Voiding again changes nothing.
     */
    public void voidBilling(Instant when) {
        if (voidedAt != null) {
            return;
        }
        this.voidedAt = Objects.requireNonNull(when, "when is required");
        links.forEach(BillingFollowUp::deactivate);
    }

    public boolean isVoided() {
        return voidedAt != null;
    }

    public Long getId() {
        return id;
    }

    public Diagnosis getDiagnosis() {
        return diagnosis;
    }

    public LocalDate getBilledOn() {
        return billedOn;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getVoidedAt() {
        return voidedAt;
    }

    public List<BillingFollowUp> getLinks() {
        return List.copyOf(links);
    }

    public List<FollowUp> getFollowUps() {
        return links.stream().map(BillingFollowUp::getFollowUp).toList();
    }
}
