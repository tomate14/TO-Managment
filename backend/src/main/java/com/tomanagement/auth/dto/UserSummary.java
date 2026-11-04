package com.tomanagement.auth.dto;

import com.tomanagement.user.entity.Role;
import com.tomanagement.user.entity.User;

public record UserSummary(Long id, String email, String fullName, Role role) {

    public static UserSummary from(User user) {
        return new UserSummary(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }
}
