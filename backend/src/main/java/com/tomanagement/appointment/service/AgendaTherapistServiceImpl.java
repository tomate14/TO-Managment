package com.tomanagement.appointment.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.tomanagement.appointment.dto.AgendaTherapistResponse;
import com.tomanagement.appointment.entity.TherapistColor;
import com.tomanagement.appointment.visibility.AppointmentVisibilityPolicyFactory;
import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.tenant.CurrentEstablishmentProvider;
import com.tomanagement.user.entity.User;
import com.tomanagement.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgendaTherapistServiceImpl implements AgendaTherapistService {

    private final UserRepository userRepository;
    private final AppointmentVisibilityPolicyFactory policyFactory;
    private final CurrentEstablishmentProvider currentEstablishment;

    public AgendaTherapistServiceImpl(UserRepository userRepository, AppointmentVisibilityPolicyFactory policyFactory,
            CurrentEstablishmentProvider currentEstablishment) {
        this.userRepository = userRepository;
        this.policyFactory = policyFactory;
        this.currentEstablishment = currentEstablishment;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgendaTherapistResponse> list(AuthenticatedUser actor) {
        Optional<Set<Long>> visible = policyFactory.forRole(actor.role()).visibleTherapists(actor, Set.of());
        // The repository order (by name) is kept inside each group: the sort is stable.
        // Only the session establishment's users (admins and therapists; the superadmin belongs to none).
        return userRepository.findAllByEstablishmentIdOrderByFullNameAsc(currentEstablishment.requireEstablishmentId())
                .stream()
                .filter(user -> visible.map(ids -> ids.contains(user.getId())).orElse(true))
                .sorted(Comparator.comparing(User::isActive).reversed())
                .map(user -> new AgendaTherapistResponse(user.getId(), user.getFullName(),
                        TherapistColor.forUser(user.getId()).getIndex(), user.isActive()))
                .toList();
    }
}
