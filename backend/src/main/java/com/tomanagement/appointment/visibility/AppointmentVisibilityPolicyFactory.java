package com.tomanagement.appointment.visibility;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.tomanagement.user.entity.Role;

import org.springframework.stereotype.Component;

/**
 * Factory for the visibility strategies; every {@link AppointmentVisibilityPolicy} bean registers itself by role.
 */
@Component
public class AppointmentVisibilityPolicyFactory {

    private final Map<Role, AppointmentVisibilityPolicy> policiesByRole = new EnumMap<>(Role.class);

    public AppointmentVisibilityPolicyFactory(List<AppointmentVisibilityPolicy> policies) {
        policies.forEach(policy -> policiesByRole.put(policy.role(), policy));
    }

    public AppointmentVisibilityPolicy forRole(Role role) {
        return policiesByRole.get(role);
    }
}
