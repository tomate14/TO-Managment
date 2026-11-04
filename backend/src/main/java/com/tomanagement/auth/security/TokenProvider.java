package com.tomanagement.auth.security;

/**
 * Port for issuing and validating access tokens. JwtTokenProvider adapts the jjwt library to it (Adapter pattern),
 * so no other class depends on the JWT library.
 */
public interface TokenProvider {

    IssuedToken issue(AuthenticatedUser user);

    /**
     * @throws com.tomanagement.common.exception.AuthenticationFailedException with TOKEN_EXPIRED when the token
     *         expired, or AUTHENTICATION_REQUIRED when it is malformed, tampered or signed with another key
     */
    AuthenticatedUser parse(String token);
}
