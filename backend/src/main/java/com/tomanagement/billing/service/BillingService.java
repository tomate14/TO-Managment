package com.tomanagement.billing.service;

import com.tomanagement.billing.dto.BillingSummaryResponse;
import com.tomanagement.billing.dto.CreateBillingRequest;

/**
 * Billing of confirmed patient appointments. Every operation answers with the updated summary.
 */
public interface BillingService {

    /**
     * @throws com.tomanagement.common.exception.ResourceNotFoundException PATIENT_NOT_FOUND
     */
    BillingSummaryResponse summary(Long patientId);

    /**
     * @throws com.tomanagement.common.exception.ResourceNotFoundException PATIENT_NOT_FOUND
     * @throws com.tomanagement.common.exception.BusinessRuleException BILLING_SESSIONS_REQUIRED,
     *         BILLING_AMOUNT_INVALID, BILLING_DATE_IN_FUTURE or BILLING_SESSION_INVALID
     */
    BillingSummaryResponse register(Long patientId, CreateBillingRequest request);

    /**
     * Voiding an already voided billing changes nothing.
     *
     * @throws com.tomanagement.common.exception.ResourceNotFoundException BILLING_NOT_FOUND
     */
    BillingSummaryResponse voidBilling(Long billingId);
}
