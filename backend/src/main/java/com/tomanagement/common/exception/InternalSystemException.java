package com.tomanagement.common.exception;

/**
 * Wraps any unexpected or third-party failure so the client only ever sees a custom, generic error.
 */
public class InternalSystemException extends ApplicationException {

    public InternalSystemException(Throwable cause) {
        super(ErrorCode.INTERNAL_ERROR, cause);
    }
}
