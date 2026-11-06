package com.tomanagement.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import com.tomanagement.auth.config.JwtProperties;
import com.tomanagement.common.exception.AuthenticationFailedException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.user.entity.Role;

import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final Instant NOW = Instant.parse("2026-11-04T12:00:00Z");
    private static final JwtProperties PROPERTIES =
            new JwtProperties("test-secret-for-unit-and-integration-tests-only-0123456789abcdef", 3_600_000L);
    private static final AuthenticatedUser USER =
            new AuthenticatedUser(7L, "ana@consultorio.com", "Ana Pérez", Role.TERAPEUTA, 3L);

    private final TokenProvider provider = new JwtTokenProvider(PROPERTIES, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void issuedTokenRoundTripsTheUserAndExpiresAfterConfiguredTime() {
        IssuedToken token = provider.issue(USER);

        assertThat(token.expiresAt()).isEqualTo(NOW.plusMillis(PROPERTIES.expirationMs()));
        assertThat(provider.parse(token.value())).isEqualTo(USER);
    }

    @Test
    void carriesTheEstablishment() {
        AuthenticatedUser parsed = provider.parse(
                provider.issue(new AuthenticatedUser(7L, "a@x.com", "Ana", Role.ADMIN, 3L)).value());

        assertThat(parsed.establishmentId()).isEqualTo(3L);
    }

    @Test
    void superadminOutsideHasNoEstablishment() {
        AuthenticatedUser parsed = provider.parse(
                provider.issue(new AuthenticatedUser(1L, "r@x.com", "Root", Role.SUPERADMIN, null)).value());

        assertThat(parsed.establishmentId()).isNull();
    }

    @Test
    void tokenWithoutEstablishmentIsRejectedForRolesThatRequireOne() {
        assertRequiresAuthentication(provider.issue(
                new AuthenticatedUser(7L, "a@x.com", "Ana", Role.ADMIN, null)).value());
        assertRequiresAuthentication(provider.issue(
                new AuthenticatedUser(8L, "b@x.com", "Beto", Role.TERAPEUTA, null)).value());
    }

    @Test
    void expiredTokenIsRejectedWithTokenExpired() {
        IssuedToken token = provider.issue(USER);
        TokenProvider later = new JwtTokenProvider(PROPERTIES,
                Clock.fixed(NOW.plus(Duration.ofHours(2)), ZoneOffset.UTC));

        assertThatThrownBy(() -> later.parse(token.value()))
                .isInstanceOf(AuthenticationFailedException.class)
                .extracting(error -> ((AuthenticationFailedException) error).getErrorCode())
                .isEqualTo(ErrorCode.TOKEN_EXPIRED);
    }

    @Test
    void tamperedTokenIsRejectedWithAuthenticationRequired() {
        String value = provider.issue(USER).value();
        String tampered = value.substring(0, value.length() - 2) + (value.endsWith("A") ? "BB" : "AA");

        assertRequiresAuthentication(tampered);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        TokenProvider other = new JwtTokenProvider(
                new JwtProperties("another-secret-that-is-also-long-enough-0123456789abcdef", 3_600_000L),
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertRequiresAuthentication(other.issue(USER).value());
    }

    @Test
    void garbageAndBlankTokensAreRejected() {
        assertRequiresAuthentication("not-a-jwt");
        assertRequiresAuthentication("");
    }

    private void assertRequiresAuthentication(String token) {
        assertThatThrownBy(() -> provider.parse(token))
                .isInstanceOf(AuthenticationFailedException.class)
                .extracting(error -> ((AuthenticationFailedException) error).getErrorCode())
                .isEqualTo(ErrorCode.AUTHENTICATION_REQUIRED);
    }
}
