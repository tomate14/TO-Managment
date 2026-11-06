package com.tomanagement.auth.dto;

import com.tomanagement.establishment.dto.EstablishmentRef;
import com.tomanagement.user.entity.Role;
import com.tomanagement.user.entity.User;

public record UserSummary(Long id, String email, String fullName, Role role, EstablishmentRef establishment) {

    public static UserSummary of(User user, EstablishmentRef establishment) {
        return new UserSummary(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), establishment);
    }
}
