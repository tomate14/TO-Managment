package com.tomanagement.auth.service;

import com.tomanagement.auth.dto.LoginRequest;
import com.tomanagement.auth.dto.LoginResponse;
import com.tomanagement.auth.dto.UserSummary;

public interface AuthService {

    /**
     * @throws com.tomanagement.common.exception.AuthenticationFailedException AUTHENTICATION_FAILED for unknown
     *         email, wrong password or inactive user (indistinguishable on purpose)
     */
    LoginResponse login(LoginRequest request);

    /**
     * @throws com.tomanagement.common.exception.AuthenticationFailedException AUTHENTICATION_REQUIRED when the user
     *         no longer exists or was deactivated
     */
    UserSummary currentUser(Long userId);
}
