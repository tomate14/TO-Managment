package com.tomanagement.common.exception;

import java.util.List;

import com.tomanagement.common.exception.dto.FieldErrorResponse;

/**
 * Base of every custom exception. The ErrorCode decides the HTTP status and the message sent to the client.
 */
public abstract class ApplicationException extends RuntimeException {

    private final ErrorCode errorCode;

    protected ApplicationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    protected ApplicationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Per-field details sent with the error; most exceptions have none.
     */
    public List<FieldErrorResponse> getFieldErrors() {
        return List.of();
    }
}
