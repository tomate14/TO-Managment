package com.tomanagement.auth.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Allowed browser origins, bound from CORS_ALLOWED_ORIGINS (comma separated, .env).
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
