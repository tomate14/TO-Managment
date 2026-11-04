package com.tomanagement.user.dto;

import com.tomanagement.user.entity.Role;
import com.tomanagement.user.entity.User;

/**
 * A user as seen by the admin screens.
 */
public record UserResponse(Long id, String email, String fullName, Role role, boolean active) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.isActive());
    }
}
