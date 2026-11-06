package com.tomanagement.user.service;

import java.time.Clock;
import java.time.Instant;

import com.tomanagement.user.config.InitialAdminProperties;
import com.tomanagement.user.entity.EmailNormalizer;
import com.tomanagement.user.entity.Role;
import com.tomanagement.user.entity.User;
import com.tomanagement.user.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InitialAdminServiceImpl implements InitialAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InitialAdminProperties properties;
    private final Clock clock;

    public InitialAdminServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
            InitialAdminProperties properties, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public InitialAdminOutcome ensureInitialAdmin() {
        if (userRepository.existsByRole(Role.SUPERADMIN)) {
            return InitialAdminOutcome.ALREADY_PRESENT;
        }
        String email = EmailNormalizer.normalize(properties.email());
        if (userRepository.findByEmail(email).isPresent()) {
            return InitialAdminOutcome.EMAIL_IN_USE;
        }
        userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(properties.password()))
                .fullName(properties.name().trim())
                .role(Role.SUPERADMIN)
                .establishmentId(null)
                .active(true)
                .createdAt(Instant.now(clock))
                .build());
        return InitialAdminOutcome.CREATED;
    }
}
