package com.tomanagement.common.exception;

public class DuplicateResourceException extends ApplicationException {

    public DuplicateResourceException() {
        super(ErrorCode.DUPLICATE_RESOURCE);
    }

    public DuplicateResourceException(ErrorCode errorCode) {
        super(errorCode);
    }
}
