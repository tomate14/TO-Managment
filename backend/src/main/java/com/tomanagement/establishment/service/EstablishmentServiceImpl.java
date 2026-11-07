package com.tomanagement.establishment.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.tomanagement.common.exception.DuplicateResourceException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.ResourceNotFoundException;
import com.tomanagement.establishment.catalog.DefaultCatalogSeeder;
import com.tomanagement.establishment.dto.CreateEstablishmentRequest;
import com.tomanagement.establishment.dto.EstablishmentAdminRequest;
import com.tomanagement.establishment.dto.EstablishmentSummaryResponse;
import com.tomanagement.establishment.dto.EstablishmentSummaryResponse.AdminRef;
import com.tomanagement.establishment.dto.RenameEstablishmentRequest;
import com.tomanagement.establishment.entity.Establishment;
import com.tomanagement.establishment.entity.EstablishmentKind;
import com.tomanagement.establishment.repository.EstablishmentRepository;
import com.tomanagement.user.entity.Role;
import com.tomanagement.user.entity.User;
import com.tomanagement.user.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EstablishmentServiceImpl implements EstablishmentService {

    private final EstablishmentRepository establishmentRepository;
    private final UserRepository userRepository;
    private final DefaultCatalogSeeder catalogSeeder;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public EstablishmentServiceImpl(EstablishmentRepository establishmentRepository, UserRepository userRepository,
            DefaultCatalogSeeder catalogSeeder, PasswordEncoder passwordEncoder, Clock clock) {
        this.establishmentRepository = establishmentRepository;
        this.userRepository = userRepository;
        this.catalogSeeder = catalogSeeder;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstablishmentSummaryResponse> list() {
        return establishmentRepository.findAllByOrderByNameAsc().stream().map(this::summarize).toList();
    }

    @Override
    @Transactional
    public EstablishmentSummaryResponse create(CreateEstablishmentRequest request) {
        EstablishmentAdminRequest adminData = request.admin();
        if (establishmentRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException(ErrorCode.ESTABLISHMENT_NAME_TAKEN);
        }
        if (userRepository.findByEmail(adminData.email()).isPresent()) {
            throw new DuplicateResourceException(ErrorCode.USER_EMAIL_TAKEN);
        }
        Establishment establishment;
        try {
            establishment = establishmentRepository
                    .saveAndFlush(new Establishment(request.name(), EstablishmentKind.INSTITUTION, Instant.now(clock)));
            catalogSeeder.seed(establishment.getId());
            userRepository.saveAndFlush(User.builder()
                    .email(adminData.email())
                    .passwordHash(passwordEncoder.encode(adminData.password()))
                    .fullName(adminData.fullName())
                    .role(Role.ADMIN)
                    .active(true)
                    .establishmentId(establishment.getId())
                    .createdAt(Instant.now(clock))
                    .build());
        } catch (DataIntegrityViolationException race) {
            throw EstablishmentViolationMapper.map(race);
        }
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
                establishment.isActive(), activeUsers, admins);
    }
}
