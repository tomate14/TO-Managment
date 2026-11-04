package com.tomanagement.auth.dto;

import java.time.Instant;

public record LoginResponse(String token, Instant expiresAt, UserSummary user) {
}
