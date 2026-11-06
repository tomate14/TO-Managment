package com.tomanagement.establishment.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomanagement.TestTable;
import com.tomanagement.TestTables;
import com.tomanagement.TestTenancy;
import com.tomanagement.TestcontainersConfiguration;
import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.common.exception.ErrorCode;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestTables.class, TestTenancy.class})
class CurrentEstablishmentControllerTest {

    private static final String CURRENT = "/api/establishment";
    private static final String HASH = "$2a$10$abcdefghijklmnopqrstuuJ8zJ2mOBs3m5yYyX0qQ2w8oVXl1K1aG";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestTables testTables;

    @Autowired
    private TestTenancy tenancy;

    @Autowired
    private ObjectMapper objectMapper;

    private Long establishmentId;
    private User admin;
    private User therapist;
    private User superadmin;

    @BeforeEach
    void seedUsers() {
        testTables.truncate(TestTable.APP_USER, TestTable.ESTABLISHMENT);
        establishmentId = tenancy.create("Consultorio").getId();
        tenancy.create("Consultorio Otro");
        admin = userRepository.save(user("admin@consultorio.com", Role.ADMIN, true, establishmentId));
        therapist = userRepository.save(user("carla@consultorio.com", Role.TERAPEUTA, true, establishmentId));
        userRepository.save(user("baja@consultorio.com", Role.TERAPEUTA, false, establishmentId));
        superadmin = userRepository.save(user("root@plataforma.com", Role.SUPERADMIN, true, null));
    }

    @Test
    void adminSeesAndRenamesOwnEstablishment() throws Exception {
        mockMvc.perform(as(admin, establishmentId, get(CURRENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(establishmentId))
                .andExpect(jsonPath("$.name").value("Consultorio"))
                .andExpect(jsonPath("$.activeUsers").value(2))
                .andExpect(jsonPath("$.createdDate").exists());

        mockMvc.perform(as(admin, establishmentId, put(CURRENT)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "Consultorio Renombrado"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Consultorio Renombrado"));
    }

    @Test
    void renamingToAnotherEstablishmentNameIsRejected() throws Exception {
        mockMvc.perform(as(admin, establishmentId, put(CURRENT)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "consultorio otro"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.ESTABLISHMENT_NAME_TAKEN.name()));
    }

    @Test
    void therapistCannotManageTheEstablishment() throws Exception {
        mockMvc.perform(as(therapist, establishmentId, get(CURRENT)))
                .andExpect(status().isForbidden());
    }

    @Test
    void superadminOutsideAnEstablishmentIsToldToPickOne() throws Exception {
        mockMvc.perform(as(superadmin, null, get(CURRENT)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.ESTABLISHMENT_REQUIRED.name()));
    }

    @Test
    void superadminInsideAnEstablishmentSeesIt() throws Exception {
        mockMvc.perform(as(superadmin, establishmentId, get(CURRENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Consultorio"));
    }

    private MockHttpServletRequestBuilder as(User user, Long sessionEstablishmentId,
            MockHttpServletRequestBuilder request) {
        String token = tenancy.tokenFor(new AuthenticatedUser(user.getId(), user.getEmail(), user.getFullName(),
                user.getRole(), sessionEstablishmentId));
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private User user(String email, Role role, boolean active, Long userEstablishmentId) {
        return User.builder()
                .email(email)
                .passwordHash(HASH)
                .fullName("Persona " + role)
                .role(role)
                .active(active)
                .establishmentId(userEstablishmentId)
                .createdAt(Instant.parse("2026-11-04T12:00:00Z"))
                .build();
    }
}
