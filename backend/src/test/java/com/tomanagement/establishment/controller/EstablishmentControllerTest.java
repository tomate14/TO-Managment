package com.tomanagement.establishment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomanagement.TestTable;
import com.tomanagement.TestTables;
import com.tomanagement.TestTenancy;
import com.tomanagement.TestcontainersConfiguration;
import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.catalog.repository.BodySegmentRepository;
import com.tomanagement.catalog.repository.ScarTypeRepository;
import com.tomanagement.catalog.repository.SurgeryTypeRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestTables.class, TestTenancy.class})
class EstablishmentControllerTest {

    private static final String ESTABLISHMENTS = "/api/establishments";
    private static final String LOGIN = "/api/auth/login";
    private static final String ME = "/api/auth/me";
    private static final String PASSWORD = "Contrasena-Segura-1";
    private static final String NEW_ADMIN_PASSWORD = "secreto123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BodySegmentRepository bodySegmentRepository;

    @Autowired
    private SurgeryTypeRepository surgeryTypeRepository;

    @Autowired
    private ScarTypeRepository scarTypeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TestTables testTables;

    @Autowired
    private TestTenancy tenancy;

    @Autowired
    private ObjectMapper objectMapper;

    private Long existingId;
    private User superadmin;
    private User existingAdmin;

    @BeforeEach
    void seedUsers() {
        testTables.truncate(TestTable.APP_USER, TestTable.ESTABLISHMENT);
        existingId = tenancy.create("Consultorio Sur").getId();
        superadmin = userRepository.save(user("root@plataforma.com", "Root", Role.SUPERADMIN, null));
        existingAdmin = userRepository.save(user("sur@consultorio.com", "Admin Sur", Role.ADMIN, existingId));
    }

    @Test
    void superadminCreatesAnEstablishmentWithItsAdminAndDefaultCatalogs() throws Exception {
        MvcResult created = mockMvc.perform(as(superadmin, post(ESTABLISHMENTS)).contentType(MediaType.APPLICATION_JSON)
                .content(json(newEstablishment("Consultorio Norte", "mg@cn.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Consultorio Norte"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.activeUsers").value(1))
                .andExpect(jsonPath("$.admins[0].email").value("mg@cn.com"))
                .andExpect(jsonPath("$.admins[0].fullName").value("Mariana Gómez"))
                .andReturn();
        Long newId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        assertThat(tenancy.callAs(newId, () -> bodySegmentRepository.findAllByOrderByNameAsc()))
                .extracting(segment -> segment.getName())
                .containsExactly("Codo", "Dedos", "Hombro", "Mano", "Muñeca");
        assertThat(tenancy.callAs(newId, () -> surgeryTypeRepository.findAllByOrderByNameAsc()))
                .extracting(type -> type.getName())
                .containsExactly("Artrodesis", "Liberación del túnel carpiano", "Osteosíntesis con placa", "Tenorrafia");
        assertThat(tenancy.callAs(newId, () -> scarTypeRepository.findAllByOrderByNameAsc()))
                .extracting(type -> type.getName())
                .containsExactly("Hipertrófica", "Normotrófica", "Queloide", "Retráctil");

        MvcResult login = mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(json(
                Map.of("email", "mg@cn.com", "password", NEW_ADMIN_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(login.getResponse().getContentAsString()).get("token").asText();
        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.establishment.id").value(newId))
                .andExpect(jsonPath("$.establishment.name").value("Consultorio Norte"));
    }

    @Test
    void duplicateNamesAndEmailsAreRejected() throws Exception {
        mockMvc.perform(as(superadmin, post(ESTABLISHMENTS)).contentType(MediaType.APPLICATION_JSON)
                .content(json(newEstablishment("CONSULTORIO SUR", "otro@cn.com"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.ESTABLISHMENT_NAME_TAKEN.name()));

        mockMvc.perform(as(superadmin, post(ESTABLISHMENTS)).contentType(MediaType.APPLICATION_JSON)
                .content(json(newEstablishment("Consultorio Norte", "sur@consultorio.com"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_EMAIL_TAKEN.name()));

        mockMvc.perform(as(superadmin, get(ESTABLISHMENTS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Consultorio Sur"));
    }

    @Test
    void onlyTheSuperadminManagesEstablishments() throws Exception {
        mockMvc.perform(as(existingAdmin, get(ESTABLISHMENTS)))
                .andExpect(status().isForbidden());

        mockMvc.perform(as(existingAdmin, post(ESTABLISHMENTS)).contentType(MediaType.APPLICATION_JSON)
                .content(json(newEstablishment("Consultorio Norte", "mg@cn.com"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void renameAndDeactivate() throws Exception {
        mockMvc.perform(as(superadmin, put(ESTABLISHMENTS + "/" + existingId)).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", "Consultorio Sur Nuevo"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Consultorio Sur Nuevo"));

        mockMvc.perform(as(superadmin, patch(ESTABLISHMENTS + "/" + existingId + "/active"))
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(as(superadmin, get(ESTABLISHMENTS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(false));

        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(json(
                Map.of("email", "sur@consultorio.com", "password", PASSWORD))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.ESTABLISHMENT_INACTIVE.name()));
    }

    @Test
    void unknownEstablishmentIsNotFound() throws Exception {
        mockMvc.perform(as(superadmin, put(ESTABLISHMENTS + "/999999")).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", "Nada"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.ESTABLISHMENT_NOT_FOUND.name()));
    }

    @Test
    void invalidDataIsRejected() throws Exception {
        mockMvc.perform(as(superadmin, post(ESTABLISHMENTS)).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", " "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()));
    }

    private Map<String, Object> newEstablishment(String name, String adminEmail) {
        Map<String, Object> admin = new LinkedHashMap<>();
        admin.put("fullName", "Mariana Gómez");
        admin.put("email", adminEmail);
        admin.put("password", NEW_ADMIN_PASSWORD);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("admin", admin);
        return body;
    }

    private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
        String token = tenancy.tokenFor(new AuthenticatedUser(user.getId(), user.getEmail(), user.getFullName(),
                user.getRole(), user.getEstablishmentId()));
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private String json(Map<String, ?> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private User user(String email, String fullName, Role role, Long establishmentId) {
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
