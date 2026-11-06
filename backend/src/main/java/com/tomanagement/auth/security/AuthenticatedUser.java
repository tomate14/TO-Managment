package com.tomanagement.auth.security;

import com.tomanagement.user.entity.Role;

/**
 * Identity carried by a valid token; it is the Spring Security principal of authenticated requests.
 */
public record AuthenticatedUser(Long id, String email, String fullName, Role role, Long establishmentId) {
}
