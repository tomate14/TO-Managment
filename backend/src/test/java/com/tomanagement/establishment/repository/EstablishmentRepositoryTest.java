package com.tomanagement.establishment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import com.tomanagement.TestTable;
import com.tomanagement.TestTables;
import com.tomanagement.TestcontainersConfiguration;
import com.tomanagement.establishment.entity.Establishment;
import com.tomanagement.user.entity.Role;
import com.tomanagement.user.entity.User;
import com.tomanagement.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/**
 * Establishment persistence and the user/establishment database rules (É8), against a real Postgres.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestTables.class})
class EstablishmentRepositoryTest {

    @Autowired
    private EstablishmentRepository establishmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestTables testTables;

    @BeforeEach
    void clean() {
        testTables.truncate(TestTable.APPOINTMENT, TestTable.APP_USER, TestTable.ESTABLISHMENT);
    }

    @Test
    void establishmentNamesAreUniqueIgnoringCase() {
        establishmentRepository.saveAndFlush(new Establishment("Consultorio Norte", Instant.now()));

        assertThat(establishmentRepository.existsByNameIgnoreCase("consultorio norte")).isTrue();
        assertThatThrownBy(() -> establishmentRepository.saveAndFlush(new Establishment("CONSULTORIO NORTE", Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void superadminHasNoEstablishmentAndOthersMustHaveOne() {
        Establishment norte = establishmentRepository.saveAndFlush(new Establishment("Norte", Instant.now()));
        userRepository.saveAndFlush(user("root@x.com", Role.SUPERADMIN, null));
        userRepository.saveAndFlush(user("ana@x.com", Role.ADMIN, norte.getId()));

        assertThatThrownBy(() -> userRepository.saveAndFlush(user("bad@x.com", Role.TERAPEUTA, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> userRepository.saveAndFlush(user("bad2@x.com", Role.SUPERADMIN, norte.getId())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private User user(String email, Role role, Long establishmentId) {
        return User.builder()
                .email(email)
                .passwordHash("hash")
                .fullName("Test User")
                .role(role)
                .establishmentId(establishmentId)
                .createdAt(Instant.now())
                .build();
    }
}
