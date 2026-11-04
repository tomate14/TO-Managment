package com.tomanagement.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT settings bound from JWT_SECRET and JWT_EXPIRATION_MS (.env).
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMs) {
}
