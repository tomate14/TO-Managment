package com.tomanagement.auth.service;

import java.util.Optional;
import java.util.UUID;

import com.tomanagement.auth.dto.LoginRequest;
import com.tomanagement.auth.dto.LoginResponse;
import com.tomanagement.auth.dto.UserSummary;
import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.auth.security.IssuedToken;
import com.tomanagement.auth.security.TokenProvider;
import com.tomanagement.common.exception.AuthenticationFailedException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.user.entity.EmailNormalizer;
import com.tomanagement.user.entity.User;
import com.tomanagement.user.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    /** Hash compared when the email is unknown, so response time does not reveal which accounts exist. */
    private final String unknownUserHash;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
            TokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
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

        IssuedToken token = tokenProvider.issue(
                new AuthenticatedUser(user.getId(), user.getEmail(), user.getFullName(), user.getRole()));
        return new LoginResponse(token.value(), token.expiresAt(), UserSummary.from(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummary currentUser(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isActive)
                .map(UserSummary::from)
                .orElseThrow(() -> new AuthenticationFailedException(ErrorCode.AUTHENTICATION_REQUIRED));
    }
}
