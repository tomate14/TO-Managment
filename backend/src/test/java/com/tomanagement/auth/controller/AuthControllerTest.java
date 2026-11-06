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
import com.tomanagement.TestTable;
import com.tomanagement.TestTables;
import com.tomanagement.TestcontainersConfiguration;
import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.auth.security.TokenProvider;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.establishment.entity.Establishment;
import com.tomanagement.establishment.repository.EstablishmentRepository;
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
@Import({TestcontainersConfiguration.class, TestTables.class})
@ExtendWith(OutputCaptureExtension.class)
class AuthControllerTest {

    private static final String LOGIN = "/api/auth/login";
    private static final String ME = "/api/auth/me";
    private static final String ESTABLISHMENT = "/api/auth/establishment";
    private static final String PASSWORD = "Contrasena-Segura-1";
    private static final Instant CREATED_AT = Instant.parse("2026-11-04T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EstablishmentRepository establishmentRepository;

    @Autowired
    private TestTables testTables;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private Long northId;

    @BeforeEach
    void seedUsers() {
        testTables.truncate(TestTable.APP_USER, TestTable.ESTABLISHMENT);
        northId = establishmentRepository.save(new Establishment("Consultorio Norte", CREATED_AT)).getId();
        userRepository.save(user("terapeuta@consultorio.com", "Ana Pérez", Role.TERAPEUTA, true));
        userRepository.save(user("inactiva@consultorio.com", "Inés Baja", Role.TERAPEUTA, false));
    }

    @Test
    void loginReturnsTheEstablishment() throws Exception {
        mockMvc.perform(login("terapeuta@consultorio.com", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.establishment.id").value(northId))
                .andExpect(jsonPath("$.user.establishment.name").value("Consultorio Norte"));
    }

    @Test
    void usersOfAnInactiveEstablishmentCannotLogIn() throws Exception {
        deactivateNorth();

        mockMvc.perform(login("terapeuta@consultorio.com", PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.ESTABLISHMENT_INACTIVE.name()));
    }

    @Test
    void superadminEntersAndLeavesAnEstablishment() throws Exception {
        userRepository.save(superadmin());
        String outside = tokenFor("root@plataforma.com");

        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, "Bearer " + outside))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.establishment").doesNotExist());

        MvcResult entered = mockMvc.perform(changeEstablishment(outside, northId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.establishment.id").value(northId))
                .andReturn();
        String inside = objectMapper.readTree(entered.getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, "Bearer " + inside))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.establishment.id").value(northId));

        mockMvc.perform(changeEstablishment(inside, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.establishment").doesNotExist());
    }

    @Test
    void onlyTheSuperadminChangesEstablishment() throws Exception {
        mockMvc.perform(changeEstablishment(tokenFor("terapeuta@consultorio.com"), northId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.name()));
    }

    @Test
    void demotedSuperadminWithAStaleTokenCannotEnterAnEstablishment() throws Exception {
        User demoted = userRepository.findByEmail("terapeuta@consultorio.com").orElseThrow();
        String staleToken = tokenProvider.issue(new AuthenticatedUser(
                demoted.getId(), demoted.getEmail(), demoted.getFullName(), Role.SUPERADMIN, null)).value();

        mockMvc.perform(changeEstablishment(staleToken, northId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.name()));
    }

    @Test
    void superadminCannotEnterAnInactiveEstablishment() throws Exception {
        userRepository.save(superadmin());
        String outside = tokenFor("root@plataforma.com");
        deactivateNorth();

        mockMvc.perform(changeEstablishment(outside, northId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.ESTABLISHMENT_INACTIVE.name()));
    }

    @Test
    void superadminCannotEnterAnUnknownEstablishment() throws Exception {
        userRepository.save(superadmin());
        String outside = tokenFor("root@plataforma.com");

        mockMvc.perform(changeEstablishment(outside, northId + 1000))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.ESTABLISHMENT_NOT_FOUND.name()));
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
    void meWithAnAdminTokenLackingAnEstablishmentReturns401() throws Exception {
        User therapist = userRepository.findByEmail("terapeuta@consultorio.com").orElseThrow();
        String legacyToken = tokenProvider.issue(new AuthenticatedUser(
                therapist.getId(), therapist.getEmail(), therapist.getFullName(), Role.ADMIN, null)).value();

        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, "Bearer " + legacyToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_REQUIRED.name()));
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

    private RequestBuilder changeEstablishment(String token, Long establishmentId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("establishmentId", establishmentId);
        return post(ESTABLISHMENT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
    }

    private void deactivateNorth() {
        Establishment north = establishmentRepository.findById(northId).orElseThrow();
        north.changeActive(false);
        establishmentRepository.save(north);
    }

    private User superadmin() {
        return User.builder()
                .email("root@plataforma.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .fullName("Root")
                .role(Role.SUPERADMIN)
                .active(true)
                .createdAt(CREATED_AT)
                .build();
    }

    private User user(String email, String fullName, Role role, boolean active) {
        return User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .fullName(fullName)
                .role(role)
                .active(active)
                .establishmentId(northId)
                .createdAt(CREATED_AT)
                .build();
    }
}
