package com.tomanagement.tenant;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Hands the tenant resolver to Hibernate, which filters every {@code @TenantId} entity by it.
 */
@Configuration
public class TenantHibernateConfig {

    @Bean
    HibernatePropertiesCustomizer tenantResolverCustomizer(AuthenticatedTenantResolver resolver) {
        return properties -> properties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
    }
}
