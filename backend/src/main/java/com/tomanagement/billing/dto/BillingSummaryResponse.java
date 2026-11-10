package com.tomanagement.billing.dto;

import java.util.List;

/**
 * Patient appointment billing state: session counts, the pending sessions (oldest first) and the billing history.
 */
public record BillingSummaryResponse(int totalSessions, int billedSessions, int pendingSessions,
        List<BilledSessionRef> pending, List<BillingResponse> billings) {
}
