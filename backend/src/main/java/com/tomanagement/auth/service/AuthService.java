package com.tomanagement.auth.service;

import com.tomanagement.auth.dto.ChangePasswordRequest;
import com.tomanagement.auth.dto.LoginRequest;
import com.tomanagement.auth.dto.LoginResponse;
import com.tomanagement.auth.dto.UserSummary;
import com.tomanagement.auth.security.AuthenticatedUser;

public interface AuthService {

    /**
     * @throws com.tomanagement.common.exception.AuthenticationFailedException AUTHENTICATION_FAILED for unknown
     *         email, wrong password or inactive user (indistinguishable on purpose)
     */
    LoginResponse login(LoginRequest request);

    /**
     * Returns the signed-in user; the establishment in the summary is the one carried by the token (so a superadmin
     * who entered an establishment sees it).
     *
     * @throws com.tomanagement.common.exception.AuthenticationFailedException AUTHENTICATION_REQUIRED when the user
     *         no longer exists or was deactivated
     */
    UserSummary currentUser(AuthenticatedUser actor);

    /**
     * Superadmin only: issues a fresh session inside the given establishment, or outside any when it is null.
     *
     * @throws com.tomanagement.common.exception.AccessDeniedApplicationException ACCESS_DENIED when the actor is not
     *         a superadmin; ESTABLISHMENT_INACTIVE when the target is inactive
     * @throws com.tomanagement.common.exception.ResourceNotFoundException ESTABLISHMENT_NOT_FOUND
     */
    LoginResponse changeEstablishment(AuthenticatedUser actor, Long establishmentId);

    /**
     * Replaces the signed-in user's password after checking the current one.
     *
     * @throws com.tomanagement.common.exception.BusinessRuleException CURRENT_PASSWORD_INCORRECT when the current
     *         password does not match (nothing is saved)
     * @throws com.tomanagement.common.exception.AuthenticationFailedException AUTHENTICATION_REQUIRED when the user
     *         no longer exists or was deactivated
     */
    void changePassword(Long userId, ChangePasswordRequest request);
}
