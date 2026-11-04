package com.tomanagement.common.exception;

public class AccessDeniedApplicationException extends ApplicationException {

    public AccessDeniedApplicationException() {
        super(ErrorCode.ACCESS_DENIED);
    }

    public AccessDeniedApplicationException(ErrorCode errorCode) {
        super(errorCode);
    }
}
