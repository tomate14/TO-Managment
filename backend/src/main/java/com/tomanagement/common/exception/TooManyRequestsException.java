package com.tomanagement.common.exception;

public class TooManyRequestsException extends ApplicationException {

    public TooManyRequestsException() {
        super(ErrorCode.TOO_MANY_REQUESTS);
    }
}
