package com.tomanagement.appointment.visibility;

import java.util.Optional;
import java.util.Set;

import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.common.exception.AccessDeniedApplicationException;
import com.tomanagement.user.entity.Role;

/**
 * Strategy that decides which therapists' appointments a user can see and manage, one per {@link Role}.
 */
public interface AppointmentVisibilityPolicy {

    Role role();

    /** Therapist ids the user may see; empty Optional = no restriction (all). */
    Optional<Set<Long>> visibleTherapists(AuthenticatedUser user, Set<Long> requested);

    /**
     * @throws AccessDeniedApplicationException when the user may not manage
     *         that therapist's agenda
     */
    void checkCanManage(AuthenticatedUser user, Long therapistId);
}
