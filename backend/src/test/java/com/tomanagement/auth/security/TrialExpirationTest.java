package com.tomanagement.auth.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import com.tomanagement.TestTable;
import com.tomanagement.TestTables;
import com.tomanagement.TestTenancy;
import com.tomanagement.TestcontainersConfiguration;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.establishment.entity.Establishment;
import com.tomanagement.establishment.repository.EstablishmentRepository;
import com.tomanagement.user.entity.Role;
import com.tomanagement.user.entity.User;
import com.tomanagement.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestTables.class, TestTenancy.class})
class TrialExpirationTest {

    private static final String ME = "/api/auth/me";
    private static final String PASSWORD = "Contrasena-Segura-1";
    private static final Instant CREATED_AT = Instant.parse("2026-11-04T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EstablishmentRepository establishmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TestTables testTables;

    @Autowired
    private TestTenancy tenancy;

    private Establishment establishment;
    private User admin;

    @BeforeEach
    void seed() {
        testTables.truncate(TestTable.APP_USER, TestTable.ESTABLISHMENT);
        establishment = tenancy.create("Vencido");
        admin = userRepository.save(user("admin@vencido.com", Role.ADMIN, establishment.getId()));
    }

    @Test
    void anOpenSessionOfAnExpiredEstablishmentIsCutOff() throws Exception {
        endTrialAt(Instant.now().minusSeconds(60));

        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOf(admin, Role.ADMIN)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.TRIAL_EXPIRED.name()));
    }

    @Test
    void aRunningTrialPassesTheFilter() throws Exception {
        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOf(admin, Role.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void theSuperadminInsideAnExpiredEstablishmentIsNotBlocked() throws Exception {
        endTrialAt(Instant.now().minusSeconds(60));
        User root = userRepository.save(user("root@plataforma.com", Role.SUPERADMIN, null));

        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOf(root, Role.SUPERADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void aRequestWithoutTokenIsNotChecked() throws Exception {
        endTrialAt(Instant.now().minusSeconds(60));

        mockMvc.perform(get(ME))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_REQUIRED.name()));
    }

    private void endTrialAt(Instant end) {
        Establishment current = establishmentRepository.findById(establishment.getId()).orElseThrow();
        current.changeTrialEnd(end);
        establishmentRepository.save(current);
    }

    /** Token scoped to the expired establishment, also for the superadmin (who is inside it). */
    private String tokenOf(User user, Role role) {
        return tenancy.tokenFor(new AuthenticatedUser(
                user.getId(), user.getEmail(), user.getFullName(), role, establishment.getId()));
    }

    private User user(String email, Role role, Long establishmentId) {
        return User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .fullName("Usuario Prueba")
                .role(role)
                .active(true)
                .establishmentId(establishmentId)
                .createdAt(CREATED_AT)
                .build();
    }
}
