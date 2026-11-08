package com.tomanagement.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.tomanagement.auth.dto.ChangePasswordRequest;
import com.tomanagement.auth.dto.LoginRequest;
import com.tomanagement.auth.dto.LoginResponse;
import com.tomanagement.auth.dto.UserSummary;
import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.auth.security.IssuedToken;
import com.tomanagement.auth.security.TokenProvider;
import com.tomanagement.common.exception.AccessDeniedApplicationException;
import com.tomanagement.common.exception.AuthenticationFailedException;
import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.ResourceNotFoundException;
import com.tomanagement.establishment.dto.EstablishmentRef;
import com.tomanagement.establishment.entity.Establishment;
import com.tomanagement.establishment.repository.EstablishmentRepository;
import com.tomanagement.user.entity.EmailNormalizer;
import com.tomanagement.user.entity.Role;
import com.tomanagement.user.entity.User;
import com.tomanagement.user.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final EstablishmentRepository establishmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final Clock clock;
    /** Hash compared when the email is unknown, so response time does not reveal which accounts exist. */
    private final String unknownUserHash;

    public AuthServiceImpl(UserRepository userRepository, EstablishmentRepository establishmentRepository,
            PasswordEncoder passwordEncoder, TokenProvider tokenProvider, Clock clock) {
        this.userRepository = userRepository;
        this.establishmentRepository = establishmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.clock = clock;
        this.unknownUserHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Optional<User> candidate = userRepository.findByEmail(EmailNormalizer.normalize(request.email()));
        String hash = candidate.map(User::getPasswordHash).orElse(unknownUserHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);

        User user = candidate
                .filter(found -> passwordMatches && found.isActive())
                .orElseThrow(AuthenticationFailedException::new);

        Establishment establishment = null;
        if (user.getEstablishmentId() != null) {
            establishment = establishmentRepository.findById(user.getEstablishmentId())
                    .orElseThrow(AuthenticationFailedException::new);
            if (!establishment.isActive()) {
                throw new AccessDeniedApplicationException(ErrorCode.ESTABLISHMENT_INACTIVE);
            }
            if (establishment.isPendingApproval()) {
                throw new AccessDeniedApplicationException(ErrorCode.ESTABLISHMENT_PENDING_APPROVAL);
            }
            if (establishment.isTrialExpired(Instant.now(clock))) {
                throw new AccessDeniedApplicationException(ErrorCode.TRIAL_EXPIRED);
            }
        }
        return session(user, establishment);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummary currentUser(AuthenticatedUser actor) {
        User user = activeUser(actor.id());
        return UserSummary.of(user, referenceOf(actor.establishmentId()));
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse changeEstablishment(AuthenticatedUser actor, Long establishmentId) {
        if (actor.role() != Role.SUPERADMIN) {
            throw new AccessDeniedApplicationException();
        }
        User user = activeUser(actor.id());
        if (user.getRole() != Role.SUPERADMIN) {
            throw new AccessDeniedApplicationException();
        }
        Establishment target = null;
        if (establishmentId != null) {
            target = establishmentRepository.findById(establishmentId)
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ESTABLISHMENT_NOT_FOUND));
            if (!target.isActive()) {
                throw new AccessDeniedApplicationException(ErrorCode.ESTABLISHMENT_INACTIVE);
            }
        }
        return session(user, target);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = activeUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException(ErrorCode.CURRENT_PASSWORD_INCORRECT);
        }
        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private User activeUser(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new AuthenticationFailedException(ErrorCode.AUTHENTICATION_REQUIRED));
    }

    private LoginResponse session(User user, Establishment establishment) {
        Long establishmentId = establishment == null ? null : establishment.getId();
        IssuedToken token = tokenProvider.issue(new AuthenticatedUser(
                user.getId(), user.getEmail(), user.getFullName(), user.getRole(), establishmentId));
        EstablishmentRef reference = establishment == null ? null : EstablishmentRef.from(establishment);
        return new LoginResponse(token.value(), token.expiresAt(), UserSummary.of(user, reference));
    }

    private EstablishmentRef referenceOf(Long establishmentId) {
        if (establishmentId == null) {
            return null;
        }
        return establishmentRepository.findById(establishmentId).map(EstablishmentRef::from).orElse(null);
    }
}
