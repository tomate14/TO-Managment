package com.tomanagement.auth.security;

import java.io.IOException;

import com.tomanagement.common.exception.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * 401 for unauthenticated requests: TOKEN_EXPIRED when the filter saw an expired token, AUTHENTICATION_REQUIRED
 * otherwise.
 */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ErrorResponseWriter writer;

    public JsonAuthenticationEntryPoint(ErrorResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        Object code = request.getAttribute(SecurityRequestAttribute.AUTHENTICATION_ERROR_CODE.key());
        writer.write(response, code instanceof ErrorCode errorCode ? errorCode : ErrorCode.AUTHENTICATION_REQUIRED);
    }
}
