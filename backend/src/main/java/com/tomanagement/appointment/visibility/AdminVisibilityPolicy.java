package com.tomanagement.appointment.visibility;

import java.util.Optional;
import java.util.Set;

import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.user.entity.Role;

import org.springframework.stereotype.Component;

/**
 * Strategy for {@link Role#ADMIN}: sees and manages every agenda.
 */
@Component
public class AdminVisibilityPolicy implements AppointmentVisibilityPolicy {

    @Override
    public Role role() {
        return Role.ADMIN;
    }

    @Override
    public Optional<Set<Long>> visibleTherapists(AuthenticatedUser user, Set<Long> requested) {
        return requested.isEmpty() ? Optional.empty() : Optional.of(requested);
    }

    @Override
    public void checkCanManage(AuthenticatedUser user, Long therapistId) {
        // An admin may manage any therapist's agenda.
    }
}
