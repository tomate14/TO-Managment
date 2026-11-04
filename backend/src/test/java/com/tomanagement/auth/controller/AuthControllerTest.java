package com.tomanagement.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomanagement.TestcontainersConfiguration;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.user.entity.Role;
import com.tomanagement.user.entity.User;
import com.tomanagement.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class AuthControllerTest {

    private static final String LOGIN = "/api/auth/login";
    private static final String ME = "/api/auth/me";
    private static final String PASSWORD = "Contrasena-Segura-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void seedUsers() {
        userRepository.deleteAll();
        userRepository.save(user("terapeuta@consultorio.com", "Ana Pérez", Role.TERAPEUTA, true));
        userRepository.save(user("inactiva@consultorio.com", "Inés Baja", Role.TERAPEUTA, false));
    }

    @Test
    void validCredentialsReturnTokenExpiryAndUser() throws Exception {
        mockMvc.perform(login("terapeuta@consultorio.com", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("terapeuta@consultorio.com"))
                .andExpect(jsonPath("$.user.fullName").value("Ana Pérez"))
                .andExpect(jsonPath("$.user.role").value("TERAPEUTA"))
                .andExpect(jsonPath("$.user.id").isNumber());
    }

    @Test
    void loginNormalizesEmailCaseAndWhitespace() throws Exception {
        mockMvc.perform(login("  Terapeuta@Consultorio.COM ", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("terapeuta@consultorio.com"));
    }

    @Test
    void failedLoginsAreIndistinguishable() throws Exception {
        String wrongPassword = errorBody(login("terapeuta@consultorio.com", "otra-cosa"));
        String unknownEmail = errorBody(login("nadie@consultorio.com", PASSWORD));
        String inactiveUser = errorBody(login("inactiva@consultorio.com", PASSWORD));

        assertThat(wrongPassword).isEqualTo(unknownEmail).isEqualTo(inactiveUser);
        assertThat(wrongPassword).contains(ErrorCode.AUTHENTICATION_FAILED.name());
    }

    @Test
    void blankFieldsReturnValidationErrorWithFieldErrors() throws Exception {
        mockMvc.perform(login("", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }

    @Test
    void malformedEmailReturnsValidationError() throws Exception {
        mockMvc.perform(login("no-es-un-email", PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
    }

    @Test
    void passwordIsNeverLogged(CapturedOutput output) throws Exception {
        mockMvc.perform(login("terapeuta@consultorio.com", "clave-que-no-debe-aparecer"));
        mockMvc.perform(login("terapeuta@consultorio.com", PASSWORD));

        assertThat(output.getAll()).doesNotContain("clave-que-no-debe-aparecer").doesNotContain(PASSWORD);
    }

    @Test
    void meReturnsTheAuthenticatedUser() throws Exception {
        String token = tokenFor("terapeuta@consultorio.com");

        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("terapeuta@consultorio.com"))
                .andExpect(jsonPath("$.fullName").value("Ana Pérez"))
                .andExpect(jsonPath("$.role").value("TERAPEUTA"));
    }

    @Test
    void meWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get(ME))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_REQUIRED.name()));
    }

    @Test
    void meForUserDeactivatedAfterLoginReturns401() throws Exception {
        String token = tokenFor("terapeuta@consultorio.com");
        User active = userRepository.findByEmail("terapeuta@consultorio.com").orElseThrow();
        userRepository.delete(active);
        userRepository.save(user("terapeuta@consultorio.com", "Ana Pérez", Role.TERAPEUTA, false));

        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_REQUIRED.name()));
    }

    private RequestBuilder login(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(new LinkedHashMap<>(Map.of(
                "email", email, "password", password)));
        return post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private String errorBody(RequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(request).andExpect(status().isUnauthorized()).andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("code").asText() + "|" + json.get("message").asText();
    }

    private String tokenFor(String email) throws Exception {
        MvcResult result = mockMvc.perform(login(email, PASSWORD)).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private User user(String email, String fullName, Role role, boolean active) {
        return User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .fullName(fullName)
                .role(role)
                .active(active)
                .createdAt(Instant.parse("2026-11-04T12:00:00Z"))
                .build();
    }
}
