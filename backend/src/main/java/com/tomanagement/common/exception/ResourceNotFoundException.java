package com.tomanagement.common.exception;

public class ResourceNotFoundException extends ApplicationException {

    public ResourceNotFoundException() {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }

    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}
