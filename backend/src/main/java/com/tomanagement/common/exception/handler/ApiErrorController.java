package com.tomanagement.common.exception.handler;

import java.time.Clock;
import java.time.Instant;

import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.dto.ErrorResponse;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Renders errors raised outside DispatcherServlet (container-level 400s, forwarded /error) as the uniform ErrorResponse.
 * Never exposes the original exception message or trace.
 */
@RestController
public class ApiErrorController implements ErrorController {

    private final Clock clock;

    public ApiErrorController(Clock clock) {
        this.clock = clock;
    }

    @RequestMapping("${server.error.path:/error}")
    public ResponseEntity<ErrorResponse> handleError(HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.fromHttpStatus(resolveStatus(request));
        ErrorResponse body = ErrorResponse.builder()
                .errorCode(errorCode)
                .timestamp(Instant.now(clock))
                .build();
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }

    private int resolveStatus(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        return status instanceof Integer code ? code : ErrorCode.INTERNAL_ERROR.getHttpStatus().value();
    }
}
