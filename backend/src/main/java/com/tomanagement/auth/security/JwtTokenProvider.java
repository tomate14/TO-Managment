package com.tomanagement.auth.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import com.tomanagement.auth.config.JwtProperties;
import com.tomanagement.common.exception.AuthenticationFailedException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.user.entity.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.stereotype.Component;

/**
 * Adapter from the jjwt library to TokenProvider. Signs with HMAC-SHA using JWT_SECRET; time comes from the injected
 * Clock so expiry is testable.
 */
@Component
public class JwtTokenProvider implements TokenProvider {

    private final SecretKey key;
    private final long expirationMs;
    private final Clock clock;
    private final JwtParser parser;

    public JwtTokenProvider(JwtProperties properties, Clock clock) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expirationMs = properties.expirationMs();
        this.clock = clock;
        this.parser = Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(clock.instant()))
                .build();
    }

    @Override
    public IssuedToken issue(AuthenticatedUser user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plusMillis(expirationMs);
        JwtBuilder builder = Jwts.builder()
                .subject(String.valueOf(user.id()))
                .claim(TokenClaim.EMAIL.getClaimName(), user.email())
                .claim(TokenClaim.FULL_NAME.getClaimName(), user.fullName())
                .claim(TokenClaim.ROLE.getClaimName(), user.role().name());
        if (user.establishmentId() != null) {
            builder.claim(TokenClaim.ESTABLISHMENT.getClaimName(), user.establishmentId());
        }
        String value = builder
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new IssuedToken(value, expiresAt);
    }

    @Override
    public AuthenticatedUser parse(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            return new AuthenticatedUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get(TokenClaim.EMAIL.getClaimName(), String.class),
                    claims.get(TokenClaim.FULL_NAME.getClaimName(), String.class),
                    Role.valueOf(claims.get(TokenClaim.ROLE.getClaimName(), String.class)),
                    Optional.ofNullable(claims.get(TokenClaim.ESTABLISHMENT.getClaimName(), Number.class))
                            .map(Number::longValue)
                            .orElse(null));
        } catch (ExpiredJwtException expired) {
            throw new AuthenticationFailedException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException invalid) {
            throw new AuthenticationFailedException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
    }
}
