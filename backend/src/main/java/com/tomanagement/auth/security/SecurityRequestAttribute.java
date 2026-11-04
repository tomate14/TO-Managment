package com.tomanagement.auth.security;

/**
 * Request attributes shared between the JWT filter and the authentication entry point.
 */
public enum SecurityRequestAttribute {

    AUTHENTICATION_ERROR_CODE;

    public String key() {
        return SecurityRequestAttribute.class.getName() + "." + name();
    }
}
