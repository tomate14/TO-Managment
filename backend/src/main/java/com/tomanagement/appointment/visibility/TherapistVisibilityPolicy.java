package com.tomanagement.appointment.visibility;

import java.util.Optional;
import java.util.Set;

import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.common.exception.AccessDeniedApplicationException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.user.entity.Role;

import org.springframework.stereotype.Component;

/**
 * Strategy for {@link Role#TERAPEUTA}: sees and manages only her own agenda.
 */
@Component
public class TherapistVisibilityPolicy implements AppointmentVisibilityPolicy {

    @Override
    public Set<Role> roles() {
        return Set.of(Role.TERAPEUTA);
    }

    @Override
    public Optional<Set<Long>> visibleTherapists(AuthenticatedUser user, Set<Long> requested) {
        return Optional.of(Set.of(user.id()));
    }

    @Override
    public void checkCanManage(AuthenticatedUser user, Long therapistId) {
        if (!therapistId.equals(user.id())) {
            throw new AccessDeniedApplicationException(ErrorCode.ACCESS_DENIED);
        }
    }
}
