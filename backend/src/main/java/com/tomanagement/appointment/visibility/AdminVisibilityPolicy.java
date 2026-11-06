package com.tomanagement.appointment.visibility;

import java.util.Optional;
import java.util.Set;

import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.user.entity.Role;

import org.springframework.stereotype.Component;

/**
 * Strategy for {@link Role#ADMIN} and for {@link Role#SUPERADMIN} inside an establishment: sees and manages every
 * agenda of that establishment.
 */
@Component
public class AdminVisibilityPolicy implements AppointmentVisibilityPolicy {

    @Override
    public Set<Role> roles() {
        return Set.of(Role.ADMIN, Role.SUPERADMIN);
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
