package com.tomanagement.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Rejects tenant-scoped requests whose token carries no establishment (a superadmin outside every establishment)
 * with {@code ESTABLISHMENT_REQUIRED} (403). Unauthenticated requests never get here: security answers 401 first.
 */
public class EstablishmentRequiredInterceptor implements HandlerInterceptor {

    private final CurrentEstablishmentProvider currentEstablishmentProvider;

    public EstablishmentRequiredInterceptor(CurrentEstablishmentProvider currentEstablishmentProvider) {
        this.currentEstablishmentProvider = currentEstablishmentProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        currentEstablishmentProvider.requireEstablishmentId();
        return true;
    }
}
