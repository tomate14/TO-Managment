package com.tomanagement.establishment.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import com.tomanagement.common.exception.DuplicateResourceException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.ResourceNotFoundException;
import com.tomanagement.establishment.dto.CreateEstablishmentRequest;
import com.tomanagement.establishment.dto.EstablishmentAdminRequest;
import com.tomanagement.establishment.dto.EstablishmentSummaryResponse;
import com.tomanagement.establishment.dto.EstablishmentSummaryResponse.AdminRef;
import com.tomanagement.establishment.dto.RenameEstablishmentRequest;
import com.tomanagement.establishment.entity.Establishment;
import com.tomanagement.establishment.entity.EstablishmentKind;
import com.tomanagement.establishment.repository.EstablishmentRepository;
import com.tomanagement.user.entity.Role;
import com.tomanagement.user.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EstablishmentServiceImpl implements EstablishmentService {

    private final EstablishmentRepository establishmentRepository;
    private final UserRepository userRepository;
    private final EstablishmentCreator establishmentCreator;
    private final Clock clock;

    public EstablishmentServiceImpl(EstablishmentRepository establishmentRepository, UserRepository userRepository,
            EstablishmentCreator establishmentCreator, Clock clock) {
        this.establishmentRepository = establishmentRepository;
        this.userRepository = userRepository;
        this.establishmentCreator = establishmentCreator;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstablishmentSummaryResponse> list() {
        return establishmentRepository.findAll().stream()
                .sorted(Comparator.comparing((Establishment e) -> !e.isPendingApproval())
                        .thenComparing(Establishment::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::summarize)
                .toList();
    }

    @Override
    @Transactional
    public EstablishmentSummaryResponse create(CreateEstablishmentRequest request) {
        EstablishmentAdminRequest adminData = request.admin();
        if (establishmentRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException(ErrorCode.ESTABLISHMENT_NAME_TAKEN);
        }
        Establishment establishment = establishmentCreator.create(new NewEstablishment(request.name(),
                EstablishmentKind.INSTITUTION, true, adminData.fullName(), adminData.email(), adminData.password()));
        return summarize(establishment);
    }

    @Override
    @Transactional
    public EstablishmentSummaryResponse approve(Long id) {
        Establishment establishment = find(id);
        establishment.approve(Instant.now(clock));
        return summarize(establishment);
    }

    @Override
    @Transactional
    public EstablishmentSummaryResponse rename(Long id, RenameEstablishmentRequest request) {
        Establishment establishment = find(id);
        if (establishmentRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new DuplicateResourceException(ErrorCode.ESTABLISHMENT_NAME_TAKEN);
        }
        establishment.rename(request.name());
        try {
            establishmentRepository.flush();
        } catch (DataIntegrityViolationException race) {
            throw EstablishmentViolationMapper.map(race);
        }
        return summarize(establishment);
    }

    @Override
    @Transactional
    public EstablishmentSummaryResponse changeActive(Long id, boolean active) {
        Establishment establishment = find(id);
        establishment.changeActive(active);
        return summarize(establishment);
    }

    private Establishment find(Long id) {
        return establishmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ESTABLISHMENT_NOT_FOUND));
    }

    private EstablishmentSummaryResponse summarize(Establishment establishment) {
        List<AdminRef> admins = userRepository
                .findByEstablishmentIdAndRoleOrderByFullNameAsc(establishment.getId(), Role.ADMIN).stream()
                .map(admin -> new AdminRef(admin.getId(), admin.getFullName(), admin.getEmail()))
                .toList();
        long activeUsers = userRepository.countByEstablishmentIdAndActiveTrue(establishment.getId());
        return new EstablishmentSummaryResponse(establishment.getId(), establishment.getName(),
                establishment.getKind(), establishment.isActive(), establishment.isPendingApproval(),
                establishment.isRejected(), establishment.getCreatedAt(), activeUsers, admins);
    }
}
