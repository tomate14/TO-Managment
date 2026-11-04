package com.tomanagement.common.exception.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tomanagement.common.exception.ErrorCode;

/**
 * Uniform error body returned by the API. Built with the Builder pattern.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public final class ErrorResponse {

    private final String code;
    private final String message;
    private final Instant timestamp;
    private final List<FieldErrorResponse> fieldErrors;

    private ErrorResponse(Builder builder) {
        this.code = builder.errorCode.name();
        this.message = builder.errorCode.getMessage();
        this.timestamp = builder.timestamp;
        this.fieldErrors = List.copyOf(builder.fieldErrors);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public List<FieldErrorResponse> getFieldErrors() {
        return fieldErrors;
    }

    public static final class Builder {

        private ErrorCode errorCode;
        private Instant timestamp;
        private final List<FieldErrorResponse> fieldErrors = new ArrayList<>();

        private Builder() {
        }

        public Builder errorCode(ErrorCode errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder fieldErrors(List<FieldErrorResponse> fieldErrors) {
            this.fieldErrors.addAll(fieldErrors);
            return this;
        }

        public ErrorResponse build() {
            Objects.requireNonNull(errorCode, "errorCode is required");
            Objects.requireNonNull(timestamp, "timestamp is required");
            return new ErrorResponse(this);
        }
    }
}
