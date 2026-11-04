package com.tomanagement.user.service;

import java.util.List;

import com.tomanagement.user.dto.CreateUserRequest;
import com.tomanagement.user.dto.UpdateUserRequest;
import com.tomanagement.user.dto.UserResponse;

/**
 * Admin management of the accounts that can sign in (H1.4).
 */
public interface UserManagementService {

    List<UserResponse> list();

    UserResponse get(Long userId);

    UserResponse create(CreateUserRequest request);

    /**
     * @param actorId the admin making the change; she cannot change her own role
     */
    UserResponse update(Long userId, UpdateUserRequest request, Long actorId);

    /**
     * @param actorId the admin making the change; she cannot deactivate herself
     */
    UserResponse changeActive(Long userId, boolean active, Long actorId);
}
