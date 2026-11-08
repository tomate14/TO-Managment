package com.tomanagement.billing.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Composite key of {@link BillingFollowUp}; both parts are filled by Hibernate from the mapped associations.
 */
@Embeddable
public class BillingFollowUpId implements Serializable {

    @Column(name = "billing_id")
    private Long billingId;

    @Column(name = "follow_up_id")
    private Long followUpId;

    protected BillingFollowUpId() {
        // Required by JPA.
    }

    public Long getBillingId() {
        return billingId;
    }

    public Long getFollowUpId() {
        return followUpId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BillingFollowUpId that)) {
            return false;
        }
        return Objects.equals(billingId, that.billingId) && Objects.equals(followUpId, that.followUpId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(billingId, followUpId);
    }
}
