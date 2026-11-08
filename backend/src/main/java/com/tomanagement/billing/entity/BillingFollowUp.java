package com.tomanagement.billing.entity;

import java.util.Objects;

import com.tomanagement.followup.entity.FollowUp;

import org.hibernate.annotations.TenantId;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/**
 * Link between a billing and a follow-up it includes. A follow-up counts as billed while it has an active link;
 * voiding the billing deactivates its links (a partial unique index allows one active link per follow-up).
 */
@Entity
@Table(name = "billing_follow_up")
public class BillingFollowUp {

    @EmbeddedId
    private BillingFollowUpId id = new BillingFollowUpId();

    @TenantId
    @Column(name = "establishment_id", nullable = false, updatable = false)
    private Long establishmentId;

    @MapsId("billingId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billing_id", nullable = false, updatable = false)
    private Billing billing;

    @MapsId("followUpId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follow_up_id", nullable = false, updatable = false)
    private FollowUp followUp;

    @Column(nullable = false)
    private boolean active;

    protected BillingFollowUp() {
        // Required by JPA.
    }

    BillingFollowUp(Billing billing, FollowUp followUp) {
        this.billing = Objects.requireNonNull(billing, "billing is required");
        this.followUp = Objects.requireNonNull(followUp, "followUp is required");
        this.active = true;
    }

    void deactivate() {
        this.active = false;
    }

    public Long getFollowUpId() {
        return id.getFollowUpId();
    }

    public FollowUp getFollowUp() {
        return followUp;
    }

    public boolean isActive() {
        return active;
    }
}
