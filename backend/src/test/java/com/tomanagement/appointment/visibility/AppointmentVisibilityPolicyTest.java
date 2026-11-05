package com.tomanagement.appointment.visibility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.common.exception.AccessDeniedApplicationException;
import com.tomanagement.user.entity.Role;

import org.junit.jupiter.api.Test;

class AppointmentVisibilityPolicyTest {

    private static final AuthenticatedUser ADMIN =
            new AuthenticatedUser(1L, "admin@example.com", "Admin", Role.ADMIN);
    private static final AuthenticatedUser THERAPIST =
            new AuthenticatedUser(2L, "therapist@example.com", "Therapist", Role.TERAPEUTA);

    private final AppointmentVisibilityPolicyFactory factory = new AppointmentVisibilityPolicyFactory(
            List.of(new AdminVisibilityPolicy(), new TherapistVisibilityPolicy()));

    @Test
    void adminSeesEveryoneOrTheRequestedTherapists() {
        AppointmentVisibilityPolicy admin = factory.forRole(Role.ADMIN);
        assertThat(admin.visibleTherapists(ADMIN, Set.of())).isEmpty();
        assertThat(admin.visibleTherapists(ADMIN, Set.of(5L, 6L))).contains(Set.of(5L, 6L));
    }

    @Test
    void aTherapistOnlySeesHerselfWhateverSheRequests() {
        assertThat(factory.forRole(Role.TERAPEUTA).visibleTherapists(THERAPIST, Set.of(99L)))
                .contains(Set.of(THERAPIST.id()));
    }

    @Test
    void aTherapistCannotManageAnotherAgenda() {
        AppointmentVisibilityPolicy policy = factory.forRole(Role.TERAPEUTA);
        assertThatCode(() -> policy.checkCanManage(THERAPIST, THERAPIST.id())).doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.checkCanManage(THERAPIST, 99L))
                .isInstanceOf(AccessDeniedApplicationException.class);
    }

    @Test
    void anAdminCanManageAnyAgenda() {
        assertThatCode(() -> factory.forRole(Role.ADMIN).checkCanManage(ADMIN, 99L)).doesNotThrowAnyException();
    }
}
