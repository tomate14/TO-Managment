package com.tomanagement.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.common.exception.AccessDeniedApplicationException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.user.entity.Role;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AuthenticatedTenantResolverTest {

    private final SecurityContextEstablishmentProvider provider = new SecurityContextEstablishmentProvider();
    private final AuthenticatedTenantResolver resolver = new AuthenticatedTenantResolver(provider);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void usesTheTokenEstablishment() {
        authenticate(new AuthenticatedUser(1L, "a", "A", Role.ADMIN, 5L));

        assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo(5L);
    }

    @Test
    void contextOverrideWins() {
        authenticate(new AuthenticatedUser(1L, "a", "A", Role.ADMIN, 5L));

        assertThat(TenantContext.callAs(9L, resolver::resolveCurrentTenantIdentifier)).isEqualTo(9L);
        assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo(5L);
    }

    @Test
    void nestedOverridesRestoreThePreviousOne() {
        Long inner = TenantContext.callAs(3L, () -> TenantContext.callAs(4L, resolver::resolveCurrentTenantIdentifier));
        Long afterInner = TenantContext.callAs(3L, () -> {
            TenantContext.runAs(4L, () -> { });
            return resolver.resolveCurrentTenantIdentifier();
        });

        assertThat(inner).isEqualTo(4L);
        assertThat(afterInner).isEqualTo(3L);
        assertThat(TenantContext.current()).isEmpty();
    }

    @Test
    void theOverrideIsRestoredWhenTheWorkFails() {
        assertThatThrownBy(() -> TenantContext.runAs(7L, () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(TenantContext.current()).isEmpty();
    }

    @Test
    void noEstablishmentMeansNone() {
        assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo(TenantSentinel.NONE.getId());
    }

    @Test
    void superadminOutsideAnEstablishmentMeansNoneAndIsRejectedWhenRequired() {
        authenticate(new AuthenticatedUser(1L, "root", "Root", Role.SUPERADMIN, null));

        assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo(TenantSentinel.NONE.getId());
        assertThatThrownBy(provider::requireEstablishmentId)
                .isInstanceOfSatisfying(AccessDeniedApplicationException.class,
                        denied -> assertThat(denied.getErrorCode()).isEqualTo(ErrorCode.ESTABLISHMENT_REQUIRED));
    }

    private static void authenticate(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }
}
