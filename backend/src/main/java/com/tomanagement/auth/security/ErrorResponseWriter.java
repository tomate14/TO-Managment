package com.tomanagement.auth.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Writes the uniform ErrorResponse from the security filter chain, where @RestControllerAdvice does not apply.
 */
@Component
public class ErrorResponseWriter {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ErrorResponseWriter(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        ErrorResponse body = ErrorResponse.builder()
                .errorCode(errorCode)
                .timestamp(Instant.now(clock))
                .build();
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
