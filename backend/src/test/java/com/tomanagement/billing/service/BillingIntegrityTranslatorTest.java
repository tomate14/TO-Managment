package com.tomanagement.billing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tomanagement.common.exception.ApplicationException;
import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.InternalSystemException;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class BillingIntegrityTranslatorTest {

    @Test
    void theActiveLinkIndexIsASessionError() {
        DataIntegrityViolationException violation = new DataIntegrityViolationException("could not execute",
                new ConstraintViolationException("dup", null, "ux_billing_follow_up_active"));

        ApplicationException translated = BillingIntegrityTranslator.translate(violation);

        assertThat(translated).isInstanceOf(BusinessRuleException.class);
        assertThat(translated.getErrorCode()).isEqualTo(ErrorCode.BILLING_SESSION_INVALID);
    }

    @Test
    void anotherConstraintIsInternal() {
        DataIntegrityViolationException violation = new DataIntegrityViolationException("could not execute",
                new ConstraintViolationException("dup", null, "some_other_constraint"));

        assertThat(BillingIntegrityTranslator.translate(violation)).isInstanceOf(InternalSystemException.class);
    }

    @Test
    void withoutAConstraintNameTheMessageDecides() {
        DataIntegrityViolationException violation = new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"ux_billing_follow_up_active\"");

        ApplicationException translated = BillingIntegrityTranslator.translate(violation);

        assertThat(translated.getErrorCode()).isEqualTo(ErrorCode.BILLING_SESSION_INVALID);
    }

    @Test
    void withoutAConstraintNameNorMatchingMessageItIsInternal() {
        DataIntegrityViolationException violation = new DataIntegrityViolationException("null value in column");

        assertThat(BillingIntegrityTranslator.translate(violation)).isInstanceOf(InternalSystemException.class);
    }
}
