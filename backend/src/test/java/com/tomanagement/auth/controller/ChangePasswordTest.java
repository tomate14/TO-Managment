package com.tomanagement.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomanagement.TestcontainersConfiguration;
import com.tomanagement.auth.dto.ChangePasswordRequest;
import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.auth.security.TokenProvider;
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

/**
 * H1.5: PUT /api/auth/password against a real Postgres.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ChangePasswordTest {

    private static final String PASSWORD_URL = "/api/auth/password";
    private static final String CURRENT = "Contrasena-Actual-1";
    private static final String NEW = "Contrasena-Nueva-2";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private User therapist;

    @BeforeEach
    void seedUser() {
        userRepository.deleteAll();
        therapist = userRepository.save(User.builder()
                .email("ana@consultorio.com")
                .passwordHash(passwordEncoder.encode(CURRENT))
                .fullName("Ana Pérez")
                .role(Role.TERAPEUTA)
                .active(true)
                .createdAt(Instant.parse("2026-11-04T12:00:00Z"))
                .build());
    }

    @Test
    void changesThePasswordSoOnlyTheNewOneLogsIn() throws Exception {
        mockMvc.perform(authenticated(put(PASSWORD_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", CURRENT, "newPassword", NEW))))
                .andExpect(status().isNoContent());

        mockMvc.perform(login(NEW)).andExpect(status().isOk());
        mockMvc.perform(login(CURRENT)).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongCurrentPasswordIsRejectedWithoutLoggingOutAndNothingChanges() throws Exception {
        mockMvc.perform(authenticated(put(PASSWORD_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", "no-es-la-actual", "newPassword", NEW))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCode.CURRENT_PASSWORD_INCORRECT.name()))
                .andExpect(jsonPath("$.message").value("La contraseña actual no es correcta."));

        assertThat(passwordEncoder.matches(CURRENT,
                userRepository.findById(therapist.getId()).orElseThrow().getPasswordHash())).isTrue();
    }

    @Test
    void newPasswordShorterThanEightCharactersIsInvalid() throws Exception {
        mockMvc.perform(authenticated(put(PASSWORD_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", CURRENT, "newPassword", "corta"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("newPassword"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(put(PASSWORD_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", CURRENT, "newPassword", NEW))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestNeverPrintsPasswords() {
        assertThat(new ChangePasswordRequest(CURRENT, NEW).toString()).doesNotContain(CURRENT).doesNotContain(NEW);
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        String token = tokenProvider.issue(new AuthenticatedUser(
                therapist.getId(), therapist.getEmail(), therapist.getFullName(), therapist.getRole())).value();
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private MockHttpServletRequestBuilder login(String password) throws Exception {
        return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", "ana@consultorio.com", "password", password)));
    }

    private String json(Map<String, ?> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }
}
