package com.tomanagement.tenant;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Guards only the tenant-scoped paths: auth, establishments administration (superadmin), health and error paths
 * stay reachable without an establishment. The configurer is a bean of this configuration (not this class itself)
 * so web slice tests, which pick up every {@code WebMvcConfigurer} class, do not need the security adapter.
 */
@Configuration
public class TenantWebConfig {

    @Bean
    WebMvcConfigurer establishmentRequiredConfigurer(CurrentEstablishmentProvider currentEstablishmentProvider) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                InterceptorRegistration registration =
                        registry.addInterceptor(new EstablishmentRequiredInterceptor(currentEstablishmentProvider));
                for (TenantScopedPath path : TenantScopedPath.values()) {
                    registration.addPathPatterns(path.getPatterns());
                }
            }
        };
    }
}
