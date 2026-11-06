package com.tomanagement;

import java.time.Instant;
import java.util.function.Supplier;

import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.auth.security.TokenProvider;
import com.tomanagement.establishment.entity.Establishment;
import com.tomanagement.establishment.repository.EstablishmentRepository;
import com.tomanagement.tenant.TenantContext;

import org.springframework.boot.test.context.TestComponent;

/**
 * Integration-test helper for multi-tenancy (É8): creates establishments, issues tokens and runs fixture work under
 * an establishment (repositories called outside a request otherwise resolve the tenant to NONE). Tests must
 * {@code @Import(TestTenancy.class)}.
 */
@TestComponent
public class TestTenancy {

    private final EstablishmentRepository establishmentRepository;
    private final TokenProvider tokenProvider;

    public TestTenancy(EstablishmentRepository establishmentRepository, TokenProvider tokenProvider) {
        this.establishmentRepository = establishmentRepository;
        this.tokenProvider = tokenProvider;
    }

    public Establishment create(String name) {
        return establishmentRepository.save(new Establishment(name, Instant.now()));
    }

    public String tokenFor(AuthenticatedUser user) {
        return tokenProvider.issue(user).value();
    }

    public void runAs(Long establishmentId, Runnable work) {
        TenantContext.runAs(establishmentId, work);
    }

    public <T> T callAs(Long establishmentId, Supplier<T> work) {
        return TenantContext.callAs(establishmentId, work);
    }
}
