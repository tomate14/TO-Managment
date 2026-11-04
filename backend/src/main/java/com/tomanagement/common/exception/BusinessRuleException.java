package com.tomanagement.common.exception;

public class BusinessRuleException extends ApplicationException {

    public BusinessRuleException() {
        super(ErrorCode.BUSINESS_RULE_VIOLATION);
    }

    public BusinessRuleException(ErrorCode errorCode) {
        super(errorCode);
    }
}
