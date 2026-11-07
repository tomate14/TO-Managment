package com.tomanagement.registration.ratelimit;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

/**
 * Finds the client address: the first X-Forwarded-For entry (the app runs behind a proxy), else the socket address.
 */
@Component
public class ClientIpResolver {

    public String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader(ForwardedHeader.X_FORWARDED_FOR.headerName());
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
