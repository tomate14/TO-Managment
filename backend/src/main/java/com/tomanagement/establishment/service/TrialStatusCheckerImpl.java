package com.tomanagement.establishment.service;

import java.time.Clock;
import java.time.Instant;

import com.tomanagement.establishment.repository.EstablishmentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrialStatusCheckerImpl implements TrialStatusChecker {

    private final EstablishmentRepository establishmentRepository;
    private final Clock clock;

    public TrialStatusCheckerImpl(EstablishmentRepository establishmentRepository, Clock clock) {
        this.establishmentRepository = establishmentRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isExpired(Long establishmentId) {
        return establishmentRepository.findTrialEndsAtById(establishmentId)
                .map(end -> !Instant.now(clock).isBefore(end))
                .orElse(false);
    }
}
