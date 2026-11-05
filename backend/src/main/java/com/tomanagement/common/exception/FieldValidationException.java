package com.tomanagement.common.exception;

import java.util.List;

import com.tomanagement.common.exception.dto.FieldErrorResponse;

/**
 * A business validation that points at specific request fields (for example, a clinical value out of range), so the
 * form can show each message under its field.
 */
public class FieldValidationException extends ApplicationException {

    private final List<FieldErrorResponse> fieldErrors;

    public FieldValidationException(ErrorCode errorCode, List<FieldErrorResponse> fieldErrors) {
        super(errorCode);
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    @Override
    public List<FieldErrorResponse> getFieldErrors() {
        return fieldErrors;
    }
}
