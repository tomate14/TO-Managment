package com.tomanagement.auth.security;

import java.io.IOException;
import java.util.List;

import com.tomanagement.common.exception.AuthenticationFailedException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates requests carrying a valid Bearer token. An invalid or expired token never fails the request here:
 * the error code is recorded and the entry point answers 401 only if the endpoint requires authentication.
 * Not a Spring bean on purpose: SecurityConfig adds it to the chain, so it is not registered twice as a servlet filter.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String ROLE_PREFIX = "ROLE_";

    private final TokenProvider tokenProvider;

    public JwtAuthenticationFilter(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        BearerToken.extract(request.getHeader(HttpHeaders.AUTHORIZATION)).ifPresent(token -> authenticate(request, token));
        chain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        try {
            AuthenticatedUser user = tokenProvider.parse(token);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    user, null, List.of(new SimpleGrantedAuthority(ROLE_PREFIX + user.role().name())));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (AuthenticationFailedException failure) {
            request.setAttribute(SecurityRequestAttribute.AUTHENTICATION_ERROR_CODE.key(), failure.getErrorCode());
        }
    }
}
