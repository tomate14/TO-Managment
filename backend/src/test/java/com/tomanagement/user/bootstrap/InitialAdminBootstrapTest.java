package com.tomanagement.user.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import com.tomanagement.TestTable;
import com.tomanagement.TestTables;
import com.tomanagement.TestTenancy;
import com.tomanagement.TestcontainersConfiguration;
import com.tomanagement.user.entity.Role;
import com.tomanagement.user.entity.User;
import com.tomanagement.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Startup admin against a real Postgres, with ADMIN_* taken from application-test.yml.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestTables.class, TestTenancy.class})
class InitialAdminBootstrapTest {

    private static final String CONFIGURED_EMAIL = "admin.inicial@consultorio.com";

    @Autowired
    private InitialAdminBootstrap bootstrap;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TestTables testTables;

    @Autowired
    private TestTenancy tenancy;

    private Long establishmentId;

    @BeforeEach
    void cleanUp() {
        testTables.truncate(TestTable.APP_USER, TestTable.ESTABLISHMENT);
        establishmentId = tenancy.create("Consultorio").getId();
    }

    @Test
    void createsTheSuperadminFromTheEnvironmentWhenThereIsNone() {
        runBootstrap();

        assertThat(userRepository.findAll()).singleElement().satisfies(superadmin -> {
            assertThat(superadmin.getEmail()).isEqualTo(CONFIGURED_EMAIL);
            assertThat(superadmin.getFullName()).isEqualTo("Admin Inicial");
            assertThat(superadmin.getRole()).isEqualTo(Role.SUPERADMIN);
            assertThat(superadmin.getEstablishmentId()).isNull();
            assertThat(superadmin.isActive()).isTrue();
            assertThat(passwordEncoder.matches("admin-inicial-test", superadmin.getPasswordHash())).isTrue();
        });
    }

    @Test
    void runningAgainDoesNotCreateASecondSuperadmin() {
        runBootstrap();
        runBootstrap();

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void doesNothingWhenAnotherSuperadminAlreadyExists() {
        userRepository.save(user("otro.root@plataforma.com", Role.SUPERADMIN, null));

        runBootstrap();

        assertThat(userRepository.findAll()).extracting(User::getEmail)
                .containsExactly("otro.root@plataforma.com");
    }

    @Test
    void anEstablishmentAdminDoesNotCountAsSuperadmin() {
        userRepository.save(user("otra.admin@consultorio.com", Role.ADMIN, establishmentId));

        runBootstrap();

        assertThat(userRepository.findAll()).extracting(User::getRole)
                .containsExactlyInAnyOrder(Role.ADMIN, Role.SUPERADMIN);
    }

    @Test
    void keepsTheExistingUserWhenTheConfiguredEmailIsTakenByATherapist() {
        userRepository.save(user(CONFIGURED_EMAIL, Role.TERAPEUTA, establishmentId));

        runBootstrap();

        assertThat(userRepository.findAll()).singleElement()
                .satisfies(existing -> assertThat(existing.getRole()).isEqualTo(Role.TERAPEUTA));
    }

    private void runBootstrap() {
        bootstrap.run(new DefaultApplicationArguments());
    }

    private User user(String email, Role role, Long establishment) {
        return User.builder()
                .email(email)
                .passwordHash("$2a$10$abcdefghijklmnopqrstuuJ8zJ2mOBs3m5yYyX0qQ2w8oVXl1K1aG")
                .fullName("Otra Persona")
                .role(role)
                .active(true)
                .establishmentId(establishment)
                .createdAt(Instant.parse("2026-11-04T12:00:00Z"))
                .build();
    }
}
