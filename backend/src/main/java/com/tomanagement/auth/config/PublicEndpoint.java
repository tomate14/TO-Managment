package com.tomanagement.auth.config;

import org.springframework.http.HttpMethod;

/**
 * Endpoints reachable without a token. Everything else requires authentication.
 */
public enum PublicEndpoint {

    LOGIN(HttpMethod.POST, "/api/auth/login"),
    HEALTH(HttpMethod.GET, "/actuator/health");

    private final HttpMethod method;
    private final String pattern;

    PublicEndpoint(HttpMethod method, String pattern) {
        this.method = method;
        this.pattern = pattern;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getPattern() {
        return pattern;
    }
}
