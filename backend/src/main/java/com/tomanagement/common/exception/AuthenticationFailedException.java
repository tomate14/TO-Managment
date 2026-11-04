package com.tomanagement.common.exception;

public class AuthenticationFailedException extends ApplicationException {

    public AuthenticationFailedException() {
        super(ErrorCode.AUTHENTICATION_FAILED);
    }

    public AuthenticationFailedException(ErrorCode errorCode) {
        super(errorCode);
    }
}
