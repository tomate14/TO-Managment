package com.tomanagement.registration.ratelimit;

/**
 * Proxy headers that carry the original client address.
 */
enum ForwardedHeader {

    X_FORWARDED_FOR("X-Forwarded-For");

    private final String headerName;

    ForwardedHeader(String headerName) {
        this.headerName = headerName;
    }

    String headerName() {
        return headerName;
    }
}
