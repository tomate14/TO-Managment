package com.tomanagement.billing.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.tomanagement.billing.entity.Billing;
import com.tomanagement.billing.entity.BillingSessionOrigin;

public record BillingResponse(Long id, LocalDate billedOn, BigDecimal amount, int sessionCount, boolean voided,
        Instant voidedAt, Instant createdAt, BillingSessionOrigin origin, List<BilledSessionRef> sessions) {

    /**
     * @param sessions the sessions of the billing, oldest first
     */
    public static BillingResponse from(Billing billing, List<BilledSessionRef> sessions) {
        return new BillingResponse(billing.getId(), billing.getBilledOn(), billing.getAmount(), sessions.size(),
                billing.isVoided(), billing.getVoidedAt(), billing.getCreatedAt(), billing.getOrigin(), sessions);
    }

    /** The amount never reaches logs. */
    @Override
    public String toString() {
        return "BillingResponse[id=" + id + ", ***]";
    }
}
