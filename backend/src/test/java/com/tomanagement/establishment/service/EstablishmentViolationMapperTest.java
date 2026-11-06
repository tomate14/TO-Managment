package com.tomanagement.establishment.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import com.tomanagement.common.exception.ApplicationException;
import com.tomanagement.common.exception.ErrorCode;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

class EstablishmentViolationMapperTest {

    @Test
    void establishmentNameIndexMapsToNameTaken() {
        ConstraintViolationException hibernate = new ConstraintViolationException("dup", new SQLException("x"),
                "insert", "ux_establishment_name_lower");

        assertThat(code(new DataIntegrityViolationException("dup", hibernate)))
                .isEqualTo(ErrorCode.ESTABLISHMENT_NAME_TAKEN);
    }

    @Test
    void emailIndexMapsToEmailTakenFromTheDriverMessage() {
        DuplicateKeyException spring = new DuplicateKeyException("dup",
                new SQLException("ERROR: duplicate key value violates unique constraint \"ux_app_user_email_lower\""));

        assertThat(code(spring)).isEqualTo(ErrorCode.USER_EMAIL_TAKEN);
    }

    @Test
    void anyOtherViolationIsAnInternalError() {
        DataIntegrityViolationException other = new DataIntegrityViolationException("null",
                new SQLException("null value in column \"name\" violates not-null constraint"));

        assertThat(code(other)).isEqualTo(ErrorCode.INTERNAL_ERROR);
    }

    private ErrorCode code(DataIntegrityViolationException violation) {
        ApplicationException mapped = EstablishmentViolationMapper.map(violation);
        return mapped.getErrorCode();
    }
}
