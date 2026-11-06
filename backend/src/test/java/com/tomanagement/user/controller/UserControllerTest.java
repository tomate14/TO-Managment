package com.tomanagement.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestTables.class, TestTenancy.class})
class UserControllerTest {

    private static final String USERS = "/api/users";
    private static final String PASSWORD = "Contrasena-Segura-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TestTables testTables;

    @Autowired
    private TestTenancy tenancy;

    @Autowired
    private ObjectMapper objectMapper;

    private Long establishmentId;
    private User admin;
    private User therapist;

    @BeforeEach
    void seedUsers() {
        testTables.truncate(TestTable.APP_USER, TestTable.ESTABLISHMENT);
        establishmentId = tenancy.create("Consultorio").getId();
        admin = userRepository.save(user("mariana@consultorio.com", "Mariana Gómez", Role.ADMIN));
        therapist = userRepository.save(user("carla@consultorio.com", "Carla Ríos", Role.TERAPEUTA));
    }

    @Test
    void adminListsUsersOrderedByName() throws Exception {
        mockMvc.perform(as(admin, get(USERS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fullName").value("Carla Ríos"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].email").value("mariana@consultorio.com"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void therapistsAreForbidden() throws Exception {
        mockMvc.perform(as(therapist, get(USERS)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.name()));
    }

    @Test
    void anonymousRequestsAreUnauthorized() throws Exception {
        mockMvc.perform(get(USERS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_REQUIRED.name()));
    }

    @Test
    void adminCreatesAUserWhoCanThenLogIn() throws Exception {
        mockMvc.perform(as(admin, post(USERS)).contentType(MediaType.APPLICATION_JSON).content(json(Map.of(
                        "email", " Paula.Vidal@Consultorio.com ", "fullName", "Paula Vidal",
                        "password", "clave-inicial", "role", "TERAPEUTA"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("paula.vidal@consultorio.com"))
                .andExpect(jsonPath("$.role").value("TERAPEUTA"))
                .andExpect(jsonPath("$.active").value(true));

        assertThat(userRepository.findByEmail("paula.vidal@consultorio.com")).get()
                .satisfies(saved -> assertThat(saved.getEstablishmentId()).isEqualTo(establishmentId));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "paula.vidal@consultorio.com", "password", "clave-inicial"))))
                .andExpect(status().isOk());
    }

    @Test
    void usersOfAnotherEstablishmentAreNotFound() throws Exception {
        Long otherEstablishmentId = tenancy.create("Otro consultorio").getId();
        User stranger = userRepository.save(User.builder()
                .email("ajena@otro.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .fullName("Ana Ajena")
                .role(Role.TERAPEUTA)
                .active(true)
                .establishmentId(otherEstablishmentId)
                .createdAt(Instant.parse("2026-11-04T12:00:00Z"))
                .build());
        String strangerUrl = USERS + "/" + stranger.getId();

        mockMvc.perform(as(admin, get(USERS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.email == 'ajena@otro.com')]").isEmpty());
        mockMvc.perform(as(admin, get(strangerUrl)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.name()));
        mockMvc.perform(as(admin, put(strangerUrl)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "Ana Cambiada", "role", "ADMIN"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.name()));
        mockMvc.perform(as(admin, patch(strangerUrl + "/active"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("active", false))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.name()));

        assertThat(userRepository.findById(stranger.getId())).get().satisfies(unchanged -> {
            assertThat(unchanged.getFullName()).isEqualTo("Ana Ajena");
            assertThat(unchanged.getRole()).isEqualTo(Role.TERAPEUTA);
            assertThat(unchanged.isActive()).isTrue();
        });
    }

    @Test
    void theSuperadminRoleCannotBeAssigned() throws Exception {
        mockMvc.perform(as(admin, post(USERS)).contentType(MediaType.APPLICATION_JSON).content(json(Map.of(
                        "email", "root2@consultorio.com", "fullName", "Otro Root",
                        "password", "clave-inicial", "role", "SUPERADMIN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("role"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Elegí Admin o Terapeuta."));
        assertThat(userRepository.findByEmail("root2@consultorio.com")).isEmpty();

        mockMvc.perform(as(admin, put(USERS + "/" + therapist.getId())).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "Carla Ríos", "role", "SUPERADMIN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("role"));
        assertThat(userRepository.findById(therapist.getId())).get()
                .satisfies(unchanged -> assertThat(unchanged.getRole()).isEqualTo(Role.TERAPEUTA));
    }

    @Test
    void duplicateEmailIsRejectedWithItsOwnCode() throws Exception {
        mockMvc.perform(as(admin, post(USERS)).contentType(MediaType.APPLICATION_JSON).content(json(Map.of(
                        "email", "CARLA@consultorio.com", "fullName", "Otra Carla",
                        "password", "clave-inicial", "role", "TERAPEUTA"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_EMAIL_TAKEN.name()))
                .andExpect(jsonPath("$.message").value("Ya hay un usuario con este email."));
    }

    @Test
    void invalidNewUserReturnsFieldErrors() throws Exception {
        mockMvc.perform(as(admin, post(USERS)).contentType(MediaType.APPLICATION_JSON).content(json(Map.of(
                        "email", "no-es-email", "fullName", " ", "password", "corta", "role", "TERAPEUTA"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.fieldErrors.length()").value(3));
    }

    @Test
    void adminEditsNameAndRole() throws Exception {
        mockMvc.perform(as(admin, put(USERS + "/" + therapist.getId())).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "Carla R. Ríos", "role", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Carla R. Ríos"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.email").value("carla@consultorio.com"));

        assertThat(userRepository.findById(therapist.getId())).get()
                .satisfies(saved -> assertThat(saved.getRole()).isEqualTo(Role.ADMIN));
    }

    @Test
    void adminCannotChangeHerOwnRole() throws Exception {
        mockMvc.perform(as(admin, put(USERS + "/" + admin.getId())).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "Mariana Gómez", "role", "TERAPEUTA"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCode.SELF_ROLE_CHANGE_FORBIDDEN.name()));
    }

    @Test
    void deactivatedUsersCannotLogIn() throws Exception {
        mockMvc.perform(as(admin, patch(USERS + "/" + therapist.getId() + "/active"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "carla@consultorio.com", "password", PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCannotDeactivateHerself() throws Exception {
        mockMvc.perform(as(admin, patch(USERS + "/" + admin.getId() + "/active"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("active", false))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCode.SELF_DEACTIVATION_FORBIDDEN.name()));
    }

    @Test
    void unknownUserIsNotFound() throws Exception {
        mockMvc.perform(as(admin, get(USERS + "/999999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.name()));
    }

    private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
        String token = tenancy.tokenFor(
                new AuthenticatedUser(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), establishmentId));
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private String json(Map<String, ?> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private User user(String email, String fullName, Role role) {
        return User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .fullName(fullName)
                .role(role)
                .active(true)
                .establishmentId(establishmentId)
                .createdAt(Instant.parse("2026-11-04T12:00:00Z"))
                .build();
    }
}
