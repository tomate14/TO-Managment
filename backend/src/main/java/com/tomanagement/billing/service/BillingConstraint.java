package com.tomanagement.billing.service;

import com.tomanagement.common.exception.ErrorCode;

/**
 * Database indexes of the billing feature whose violation has a meaning for the user.
 */
enum BillingConstraint {

    ACTIVE_FOLLOW_UP_LINK("ux_billing_follow_up_active", ErrorCode.BILLING_SESSION_INVALID),
    ACTIVE_APPOINTMENT_LINK("ux_billing_appointment_active", ErrorCode.BILLING_SESSION_INVALID);

    private final String indexName;
    private final ErrorCode errorCode;

    BillingConstraint(String indexName, ErrorCode errorCode) {
        this.indexName = indexName;
        this.errorCode = errorCode;
    }

    String getIndexName() {
        return indexName;
    }

    ErrorCode getErrorCode() {
        return errorCode;
    }
}
