package com.tomanagement.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.tomanagement.user.config.InitialAdminProperties;
import com.tomanagement.user.entity.Role;
import com.tomanagement.user.entity.User;
import com.tomanagement.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class InitialAdminServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-04T12:00:00Z");

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private InitialAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode("secreta-inicial")).thenReturn("bcrypt-hash");
        InitialAdminProperties properties =
                new InitialAdminProperties("  Admin@Consultorio.COM ", "secreta-inicial", " Mariana Gómez ");
        service = new InitialAdminServiceImpl(userRepository, passwordEncoder, properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsTheSuperadminWhenThereIsNone() {
        when(userRepository.existsByRole(Role.SUPERADMIN)).thenReturn(false);
        when(userRepository.findByEmail("admin@consultorio.com")).thenReturn(Optional.empty());

        assertThat(service.ensureInitialAdmin()).isEqualTo(InitialAdminOutcome.CREATED);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue()).satisfies(user -> {
            assertThat(user.getEmail()).isEqualTo("admin@consultorio.com");
            assertThat(user.getPasswordHash()).isEqualTo("bcrypt-hash");
            assertThat(user.getFullName()).isEqualTo("Mariana Gómez");
            assertThat(user.getRole()).isEqualTo(Role.SUPERADMIN);
            assertThat(user.getEstablishmentId()).isNull();
            assertThat(user.isActive()).isTrue();
            assertThat(user.getCreatedAt()).isEqualTo(NOW);
        });
    }

    @Test
    void doesNothingWhenASuperadminAlreadyExists() {
        when(userRepository.existsByRole(Role.SUPERADMIN)).thenReturn(true);

        assertThat(service.ensureInitialAdmin()).isEqualTo(InitialAdminOutcome.ALREADY_PRESENT);

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any(String.class));
    }

    @Test
    void doesNotCreateTheAdminWhenTheEmailBelongsToAnotherUser() {
        when(userRepository.existsByRole(Role.SUPERADMIN)).thenReturn(false);
        when(userRepository.findByEmail("admin@consultorio.com")).thenReturn(Optional.of(mock(User.class)));

        assertThat(service.ensureInitialAdmin()).isEqualTo(InitialAdminOutcome.EMAIL_IN_USE);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void propertiesNeverPrintTheirValues() {
        assertThat(new InitialAdminProperties("a@b.com", "secreta-inicial", "Ana").toString())
                .doesNotContain("secreta-inicial")
                .doesNotContain("a@b.com");
    }
}
