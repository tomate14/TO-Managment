package com.tomanagement.appointment.visibility;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.tomanagement.common.exception.InternalSystemException;
import com.tomanagement.user.entity.Role;

import org.springframework.stereotype.Component;

/**
 * Factory for the visibility strategies; every {@link AppointmentVisibilityPolicy} bean registers itself by role.
 */
@Component
public class AppointmentVisibilityPolicyFactory {

    private final Map<Role, AppointmentVisibilityPolicy> policiesByRole = new EnumMap<>(Role.class);

    public AppointmentVisibilityPolicyFactory(List<AppointmentVisibilityPolicy> policies) {
        policies.forEach(policy -> policy.roles().forEach(role -> register(role, policy)));
        for (Role role : Role.values()) {
            if (!policiesByRole.containsKey(role)) {
                throw new InternalSystemException(
                        new IllegalStateException("No visibility policy is mapped to role " + role));
            }
        }
    }

    private void register(Role role, AppointmentVisibilityPolicy policy) {
        if (policiesByRole.put(role, policy) != null) {
            throw new InternalSystemException(
                    new IllegalStateException("Role " + role + " is mapped to more than one visibility policy"));
        }
    }

    public AppointmentVisibilityPolicy forRole(Role role) {
        return policiesByRole.get(role);
    }
}
