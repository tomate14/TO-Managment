package com.tomanagement.common.exception.handler;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.tomanagement.common.exception.ApplicationException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.InternalSystemException;
import com.tomanagement.common.exception.dto.ErrorResponse;
import com.tomanagement.common.exception.dto.FieldErrorResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Single translation point from any exception to the uniform ErrorResponse.
 * Unexpected exceptions are wrapped in InternalSystemException so internal details never reach the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> handleApplicationException(ApplicationException exception) {
        LOGGER.warn("Application error: {}", exception.getErrorCode());
        return respond(exception.getErrorCode(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<FieldErrorResponse> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
                .toList();
        return respond(ErrorCode.VALIDATION_ERROR, fieldErrors);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception exception) {
        return respond(ErrorCode.MALFORMED_REQUEST, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        return respond(ErrorCode.UNSUPPORTED_MEDIA_TYPE, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleUnknownEndpoint(NoResourceFoundException exception) {
        return respond(ErrorCode.ENDPOINT_NOT_FOUND, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        return respond(ErrorCode.METHOD_NOT_ALLOWED, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        InternalSystemException wrapped = new InternalSystemException(exception);
        LOGGER.error("Unexpected error", wrapped);
        return respond(wrapped.getErrorCode(), List.of());
    }

    private ResponseEntity<ErrorResponse> respond(ErrorCode errorCode, List<FieldErrorResponse> fieldErrors) {
        ErrorResponse body = ErrorResponse.builder()
                .errorCode(errorCode)
                .timestamp(Instant.now(clock))
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }
}
