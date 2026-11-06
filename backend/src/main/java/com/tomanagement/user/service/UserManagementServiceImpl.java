package com.tomanagement.user.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.DuplicateResourceException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.ResourceNotFoundException;
import com.tomanagement.tenant.CurrentEstablishmentProvider;
import com.tomanagement.user.dto.CreateUserRequest;
import com.tomanagement.user.dto.UpdateUserRequest;
import com.tomanagement.user.dto.UserResponse;
import com.tomanagement.user.entity.User;
import com.tomanagement.user.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User management inside the session's establishment: users of another establishment (or the superadmin) are never
 * listed, read or changed, and new users always join the session's establishment.
 */
@Service
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentEstablishmentProvider currentEstablishment;
    private final Clock clock;

    public UserManagementServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
            CurrentEstablishmentProvider currentEstablishment, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentEstablishment = currentEstablishment;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return userRepository.findAllByEstablishmentIdOrderByFullNameAsc(currentEstablishment.requireEstablishmentId())
                .stream().map(UserResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse get(Long userId) {
        return UserResponse.from(find(userId));
    }

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        Long establishmentId = currentEstablishment.requireEstablishmentId();
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new DuplicateResourceException(ErrorCode.USER_EMAIL_TAKEN);
        }
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(request.role())
                .active(true)
                .establishmentId(establishmentId)
                .createdAt(Instant.now(clock))
                .build();
        try {
            return UserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException raceOnUniqueEmail) {
            throw new DuplicateResourceException(ErrorCode.USER_EMAIL_TAKEN);
        }
    }

    @Override
    @Transactional
    public UserResponse update(Long userId, UpdateUserRequest request, Long actorId) {
        User user = find(userId);
        if (user.getId().equals(actorId) && user.getRole() != request.role()) {
            throw new BusinessRuleException(ErrorCode.SELF_ROLE_CHANGE_FORBIDDEN);
        }
        user.updateProfile(request.fullName(), request.role());
        return UserResponse.from(user);
    }

    @Override
    @Transactional
    public UserResponse changeActive(Long userId, boolean active, Long actorId) {
        User user = find(userId);
        if (user.getId().equals(actorId) && !active) {
            throw new BusinessRuleException(ErrorCode.SELF_DEACTIVATION_FORBIDDEN);
        }
        user.changeActive(active);
        return UserResponse.from(user);
    }

    private User find(Long userId) {
        return userRepository.findByIdAndEstablishmentId(userId, currentEstablishment.requireEstablishmentId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
    }
}
