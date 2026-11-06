package com.tomanagement.establishment.service;

import java.time.Clock;

import com.tomanagement.common.exception.DuplicateResourceException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.ResourceNotFoundException;
import com.tomanagement.establishment.dto.CurrentEstablishmentResponse;
import com.tomanagement.establishment.dto.RenameEstablishmentRequest;
import com.tomanagement.establishment.entity.Establishment;
import com.tomanagement.establishment.repository.EstablishmentRepository;
import com.tomanagement.tenant.CurrentEstablishmentProvider;
import com.tomanagement.user.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentEstablishmentServiceImpl implements CurrentEstablishmentService {

    private final EstablishmentRepository establishmentRepository;
    private final UserRepository userRepository;
    private final CurrentEstablishmentProvider currentEstablishment;
    private final Clock clock;

    public CurrentEstablishmentServiceImpl(EstablishmentRepository establishmentRepository,
            UserRepository userRepository, CurrentEstablishmentProvider currentEstablishment, Clock clock) {
        this.establishmentRepository = establishmentRepository;
        this.userRepository = userRepository;
        this.currentEstablishment = currentEstablishment;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentEstablishmentResponse get() {
        return toResponse(find(currentEstablishment.requireEstablishmentId()));
    }

    @Override
    @Transactional
    public CurrentEstablishmentResponse rename(RenameEstablishmentRequest request) {
        Long id = currentEstablishment.requireEstablishmentId();
        Establishment establishment = find(id);
        if (establishmentRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new DuplicateResourceException(ErrorCode.ESTABLISHMENT_NAME_TAKEN);
        }
        establishment.rename(request.name());
        try {
            establishmentRepository.flush();
        } catch (DataIntegrityViolationException raceOnUniqueName) {
            throw new DuplicateResourceException(ErrorCode.ESTABLISHMENT_NAME_TAKEN);
        }
        return toResponse(establishment);
    }

    private Establishment find(Long id) {
        return establishmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ESTABLISHMENT_NOT_FOUND));
    }

    private CurrentEstablishmentResponse toResponse(Establishment establishment) {
        return new CurrentEstablishmentResponse(establishment.getId(), establishment.getName(),
                userRepository.countByEstablishmentIdAndActiveTrue(establishment.getId()),
                establishment.getCreatedAt().atZone(clock.getZone()).toLocalDate());
    }
}
