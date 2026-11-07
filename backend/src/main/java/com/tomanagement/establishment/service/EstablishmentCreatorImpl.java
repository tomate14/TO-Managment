package com.tomanagement.establishment.service;

import java.time.Clock;
import java.time.Instant;

import com.tomanagement.common.exception.DuplicateResourceException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.establishment.catalog.DefaultCatalogSeeder;
import com.tomanagement.establishment.config.TrialProperties;
import com.tomanagement.establishment.entity.Establishment;
import com.tomanagement.establishment.repository.EstablishmentRepository;
import com.tomanagement.user.entity.Role;
import com.tomanagement.user.entity.User;
import com.tomanagement.user.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EstablishmentCreatorImpl implements EstablishmentCreator {

    private final EstablishmentRepository establishmentRepository;
    private final UserRepository userRepository;
    private final DefaultCatalogSeeder catalogSeeder;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final TrialProperties trialProperties;

    public EstablishmentCreatorImpl(EstablishmentRepository establishmentRepository, UserRepository userRepository,
            DefaultCatalogSeeder catalogSeeder, PasswordEncoder passwordEncoder, Clock clock,
            TrialProperties trialProperties) {
        this.establishmentRepository = establishmentRepository;
        this.userRepository = userRepository;
        this.catalogSeeder = catalogSeeder;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.trialProperties = trialProperties;
    }

    @Override
    @Transactional
    public Establishment create(NewEstablishment request) {
        if (userRepository.findByEmail(request.adminEmail()).isPresent()) {
            throw new DuplicateResourceException(ErrorCode.USER_EMAIL_TAKEN);
        }
        try {
            Instant now = Instant.now(clock);
            Establishment establishment = new Establishment(request.name(), request.kind(), now);
            if (request.approved()) {
                establishment.approve(now, trialProperties.trial());
            }
            establishment = establishmentRepository.saveAndFlush(establishment);
            catalogSeeder.seed(establishment.getId());
            userRepository.saveAndFlush(User.builder()
                    .email(request.adminEmail())
                    .passwordHash(passwordEncoder.encode(request.adminPassword()))
                    .fullName(request.adminFullName())
                    .role(Role.ADMIN)
                    .active(true)
                    .establishmentId(establishment.getId())
                    .createdAt(now)
                    .build());
            return establishment;
        } catch (DataIntegrityViolationException race) {
            throw EstablishmentViolationMapper.map(race);
        }
    }
}
