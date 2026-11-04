package com.tomanagement.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import com.tomanagement.TestcontainersConfiguration;
import com.tomanagement.user.entity.Role;
import com.tomanagement.user.entity.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void savesUserWithNormalizedEmailAndFindsItByEmail() {
        userRepository.save(user("  Ana@Consultorio.COM ", Role.TERAPEUTA));

        assertThat(userRepository.findByEmail("ana@consultorio.com"))
                .get()
                .satisfies(found -> {
                    assertThat(found.getId()).isNotNull();
                    assertThat(found.getEmail()).isEqualTo("ana@consultorio.com");
                    assertThat(found.getRole()).isEqualTo(Role.TERAPEUTA);
                    assertThat(found.isActive()).isTrue();
                });
    }

    @Test
    void rejectsDuplicateEmailRegardlessOfCase() {
        userRepository.saveAndFlush(user("ana@consultorio.com", Role.ADMIN));

        assertThatThrownBy(() -> userRepository.saveAndFlush(user("ANA@consultorio.com", Role.TERAPEUTA)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void returnsEmptyForUnknownEmail() {
        assertThat(userRepository.findByEmail("nadie@consultorio.com")).isEmpty();
    }

    private static User user(String email, Role role) {
        return User.builder()
                .email(email)
                .passwordHash("$2a$10$abcdefghijklmnopqrstuuJ8zJ2mOBs3m5yYyX0qQ2w8oVXl1K1aG")
                .fullName("Ana Pérez")
                .role(role)
                .active(true)
                .createdAt(Instant.parse("2026-11-04T12:00:00Z"))
                .build();
    }
}
