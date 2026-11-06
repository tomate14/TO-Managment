package com.tomanagement.establishment.service;

import java.util.Arrays;
import java.util.Optional;

import com.tomanagement.common.exception.ErrorCode;

/**
 * Unique indexes whose violation has a user-facing meaning when creating or renaming establishments.
 */
public enum EstablishmentConstraint {
    ESTABLISHMENT_NAME("ux_establishment_name_lower", ErrorCode.ESTABLISHMENT_NAME_TAKEN),
    USER_EMAIL("ux_app_user_email_lower", ErrorCode.USER_EMAIL_TAKEN);

    private final String indexName;
    private final ErrorCode errorCode;

    EstablishmentConstraint(String indexName, ErrorCode errorCode) {
        this.indexName = indexName;
        this.errorCode = errorCode;
    }

    public String getIndexName() {
        return indexName;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public static Optional<EstablishmentConstraint> fromIndexName(String name) {
        return Arrays.stream(values()).filter(constraint -> constraint.indexName.equals(name)).findFirst();
    }
}
