package com.tomanagement.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomanagement.TestTable;
import com.tomanagement.TestTables;
import com.tomanagement.TestTenancy;
import com.tomanagement.TestcontainersConfiguration;
import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.diagnosis.entity.SurgicalTreatment;
import com.tomanagement.diagnosis.entity.Treatment;
import com.tomanagement.diagnosis.repository.DiagnosisRepository;
import com.tomanagement.followup.entity.FollowUp;
import com.tomanagement.followup.repository.FollowUpRepository;
import com.tomanagement.user.entity.Role;
import com.tomanagement.user.entity.User;
import com.tomanagement.user.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Cross-establishment isolation (É8) end to end: two establishments built through the API with their own admins; no
 * read, write or uniqueness rule ever crosses from one to the other.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestTables.class, TestTenancy.class})
class CrossEstablishmentIsolationTest {

    private static final String PASSWORD = "Contrasena-Segura-1";
    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestTables testTables;

    @Autowired
    private TestTenancy tenancy;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private FollowUpRepository followUpRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Clock clock;

    private User superadmin;
    private Fixture a;
    private Fixture b;

    /** Everything one establishment owns, created through the API with its admin's token. */
    private record Fixture(Long id, String adminToken, Long adminId, Long therapistId, Long patientId,
            Long surgeryTypeId, Long scarTypeId, List<Long> segmentIds, Long wristId, Long diagnosisId,
            Long templateId, Long appointmentId, Long followUpId) {
    }

    @BeforeEach
    void seed() throws Exception {
        testTables.truncate(TestTable.APPOINTMENT, TestTable.FOLLOW_UP, TestTable.DIAGNOSIS, TestTable.TREATMENT,
                TestTable.PATIENT, TestTable.EVALUATION_TEMPLATE, TestTable.BODY_SEGMENT, TestTable.SURGERY_TYPE,
                TestTable.SCAR_TYPE, TestTable.APP_USER, TestTable.ESTABLISHMENT);
        superadmin = userRepository.save(User.builder()
                .email("root@plataforma.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .fullName("Root")
                .role(Role.SUPERADMIN)
                .active(true)
                .createdAt(Instant.parse("2026-11-04T12:00:00Z"))
                .build());
        a = establishment("Consultorio A", "admin@a.com", "terapeuta@a.com", "30111222");
        b = establishment("Consultorio B", "admin@b.com", "terapeuta@b.com", "30333444");
    }

    @Test
    void readsNeverCrossEstablishments() throws Exception {
        expectError(perform(a.adminToken(), get("/api/patients/" + b.patientId())), ErrorCode.PATIENT_NOT_FOUND);
        expectError(perform(a.adminToken(), get("/api/diagnoses/" + b.diagnosisId())), ErrorCode.DIAGNOSIS_NOT_FOUND);
        expectError(perform(a.adminToken(), get("/api/templates/" + b.templateId())), ErrorCode.TEMPLATE_NOT_FOUND);
        expectError(perform(a.adminToken(), get("/api/appointments/" + b.appointmentId())),
                ErrorCode.APPOINTMENT_NOT_FOUND);
        expectError(perform(a.adminToken(), get("/api/diagnoses/" + b.diagnosisId() + "/follow-ups")),
                ErrorCode.DIAGNOSIS_NOT_FOUND);

        JsonNode patients = read(perform(a.adminToken(), get("/api/patients").param("q", "")));
        assertThat(ids(patients.get("content"))).containsExactly(a.patientId());

        JsonNode segments = read(perform(a.adminToken(), get("/api/catalogs/body-segments")));
        assertThat(ids(segments)).hasSize(5).containsExactlyInAnyOrderElementsOf(a.segmentIds())
                .doesNotContainAnyElementsOf(b.segmentIds());

        JsonNode therapists = read(perform(a.adminToken(), get("/api/agenda/therapists")));
        assertThat(ids(therapists)).containsExactlyInAnyOrder(a.adminId(), a.therapistId());

        JsonNode users = read(perform(a.adminToken(), get("/api/users")));
        assertThat(ids(users)).containsExactlyInAnyOrder(a.adminId(), a.therapistId());
    }

    @Test
    void theJoinedTreatmentRootAndTheFollowUpChildrenOfAnotherEstablishmentAreInvisible() {
        Long treatmentOfB = tenancy.callAs(b.id(),
                () -> diagnosisRepository.findById(b.diagnosisId()).orElseThrow().getTreatment().getId());

        Treatment rootSeenByA = tenancy.callAs(a.id(),
                () -> inSession(em -> em.find(Treatment.class, treatmentOfB)));
        SurgicalTreatment subtypeSeenByA = tenancy.callAs(a.id(),
                () -> inSession(em -> em.find(SurgicalTreatment.class, treatmentOfB)));
        List<Long> treatmentIdsSeenByA = tenancy.callAs(a.id(), () -> inSession(em -> em
                .createQuery("select t.id from Treatment t", Long.class).getResultList()));
        Treatment rootSeenByB = tenancy.callAs(b.id(),
                () -> inSession(em -> em.find(Treatment.class, treatmentOfB)));
        assertThat(rootSeenByA).isNull();
        assertThat(subtypeSeenByA).isNull();
        assertThat(treatmentIdsSeenByA).doesNotContain(treatmentOfB);
        assertThat(rootSeenByB).isInstanceOf(SurgicalTreatment.class);

        Optional<FollowUp> followUpSeenByA = tenancy.callAs(a.id(), () -> followUpRepository.findById(b.followUpId()));
        Optional<FollowUp> followUpSeenByB = tenancy.callAs(b.id(), () -> followUpRepository.findById(b.followUpId()));
        List<Long> evaluationsSeenByA = tenancy.callAs(a.id(), () -> inSession(em -> em
                .createQuery("select e.id from SegmentEvaluation e", Long.class).getResultList()));
        List<Long> evaluationsSeenByB = tenancy.callAs(b.id(), () -> inSession(em -> em
                .createQuery("select e.id from SegmentEvaluation e", Long.class).getResultList()));
        assertThat(followUpSeenByA).isEmpty();
        assertThat(followUpSeenByB).isPresent();
        assertThat(evaluationsSeenByA).isEmpty();
        assertThat(evaluationsSeenByB).hasSize(1);
    }

    @Test
    void writesCannotReferenceAnotherEstablishment() throws Exception {
        String date = appointmentDate().toString();
        expectError(perform(a.adminToken(), post("/api/appointments").contentType(JSON)
                .content(json(appointment(b.therapistId(), a.patientId(), date, "11:00")))),
                ErrorCode.USER_NOT_FOUND);
        expectError(perform(a.adminToken(), post("/api/appointments").contentType(JSON)
                .content(json(appointment(a.therapistId(), b.patientId(), date, "11:00")))),
                ErrorCode.PATIENT_NOT_FOUND);
        expectError(perform(a.adminToken(), post("/api/patients/" + a.patientId() + "/diagnoses").contentType(JSON)
                .content(json(surgicalDiagnosis(b.surgeryTypeId())))), ErrorCode.CATALOG_ITEM_NOT_FOUND);
        expectError(perform(a.adminToken(), post("/api/diagnoses/" + a.diagnosisId() + "/follow-ups")
                .contentType(JSON).content(json(followUp(b.scarTypeId(), a.wristId())))),
                ErrorCode.CATALOG_ITEM_NOT_FOUND);
        expectError(perform(a.adminToken(), put("/api/users/" + b.therapistId()).contentType(JSON)
                .content(json(Map.of("fullName", "Robada", "role", "ADMIN")))), ErrorCode.USER_NOT_FOUND);

        assertThat(ids(read(perform(a.adminToken(), get("/api/diagnoses/" + a.diagnosisId() + "/follow-ups")))))
                .isEmpty();
        assertThat(userRepository.findById(b.therapistId())).get().satisfies(unchanged -> {
            assertThat(unchanged.getFullName()).isEqualTo("Terapeuta");
            assertThat(unchanged.getRole()).isEqualTo(Role.TERAPEUTA);
        });
    }

    @Test
    void uniquenessIsPerEstablishment() throws Exception {
        perform(a.adminToken(), post("/api/patients").contentType(JSON).content(json(patient("28999111"))))
                .andExpect(status().isCreated());
        perform(b.adminToken(), post("/api/patients").contentType(JSON).content(json(patient("28999111"))))
                .andExpect(status().isCreated());
        expectError(perform(a.adminToken(), post("/api/patients").contentType(JSON).content(json(patient("28999111")))),
                ErrorCode.PATIENT_DNI_DUPLICATE);

        expectError(perform(a.adminToken(), post("/api/catalogs/body-segments").contentType(JSON)
                .content(json(Map.of("name", "codo")))), ErrorCode.CATALOG_ITEM_DUPLICATE);
        perform(a.adminToken(), post("/api/catalogs/body-segments").contentType(JSON)
                .content(json(Map.of("name", "Antebrazo")))).andExpect(status().isCreated());
        perform(b.adminToken(), post("/api/catalogs/body-segments").contentType(JSON)
                .content(json(Map.of("name", "Antebrazo")))).andExpect(status().isCreated());
    }

    @Test
    void superadminInsideActsAsAdminOnlyThere() throws Exception {
        String outside = tenancy.tokenFor(new AuthenticatedUser(superadmin.getId(), superadmin.getEmail(),
                superadmin.getFullName(), Role.SUPERADMIN, null));
        expectError(perform(outside, get("/api/patients")), ErrorCode.ESTABLISHMENT_REQUIRED);

        Map<String, Object> enter = new HashMap<>();
        enter.put("establishmentId", a.id());
        String inside = read(perform(outside, post("/api/auth/establishment").contentType(JSON).content(json(enter))))
                .get("token").asText();

        assertThat(ids(read(perform(inside, get("/api/patients"))).get("content"))).containsExactly(a.patientId());
        expectError(perform(inside, get("/api/patients/" + b.patientId())), ErrorCode.PATIENT_NOT_FOUND);
        perform(inside, get("/api/appointments/" + a.appointmentId())).andExpect(status().isOk());
        assertThat(ids(read(perform(inside, get("/api/agenda/therapists")))))
                .containsExactlyInAnyOrder(a.adminId(), a.therapistId());

        JsonNode created = created(perform(inside, post("/api/users").contentType(JSON).content(json(Map.of(
                "email", "segunda@a.com", "fullName", "Segunda Admin", "password", "clave-inicial",
                "role", "ADMIN")))));
        Long createdId = created.get("id").asLong();
        assertThat(userRepository.findById(createdId)).get()
                .satisfies(user -> assertThat(user.getEstablishmentId()).isEqualTo(a.id()));
        assertThat(ids(read(perform(a.adminToken(), get("/api/users"))))).contains(createdId);
        assertThat(ids(read(perform(b.adminToken(), get("/api/users"))))).doesNotContain(createdId);
    }

    @Test
    void adminCannotCreateASuperadmin() throws Exception {
        perform(a.adminToken(), post("/api/users").contentType(JSON).content(json(Map.of(
                        "email", "root2@a.com", "fullName", "Root Falso", "password", "clave-inicial",
                        "role", "SUPERADMIN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("role"));
        assertThat(userRepository.findByEmail("root2@a.com")).isEmpty();
    }

    private Fixture establishment(String name, String adminEmail, String therapistEmail, String dni)
            throws Exception {
        String superadminToken = tenancy.tokenFor(new AuthenticatedUser(superadmin.getId(), superadmin.getEmail(),
                superadmin.getFullName(), Role.SUPERADMIN, null));
        Map<String, Object> admin = new LinkedHashMap<>();
        admin.put("fullName", "Admin");
        admin.put("email", adminEmail);
        admin.put("password", PASSWORD);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("admin", admin);
        Long establishmentId = created(perform(superadminToken, post("/api/establishments").contentType(JSON)
                .content(json(body)))).get("id").asLong();

        JsonNode login = read(mockMvc.perform(post("/api/auth/login").contentType(JSON)
                .content(json(Map.of("email", adminEmail, "password", PASSWORD)))));
        String token = login.get("token").asText();
        Long adminId = login.get("user").get("id").asLong();

        Long therapistId = created(perform(token, post("/api/users").contentType(JSON).content(json(Map.of(
                "email", therapistEmail, "fullName", "Terapeuta", "password", PASSWORD, "role", "TERAPEUTA")))))
                .get("id").asLong();
        Long patientId = created(perform(token, post("/api/patients").contentType(JSON).content(json(patient(dni)))))
                .get("id").asLong();

        Long surgeryTypeId = catalogId(token, "surgery-types", "Osteosíntesis con placa");
        Long scarTypeId = catalogId(token, "scar-types", "Normotrófica");
        List<Long> segmentIds = ids(read(perform(token, get("/api/catalogs/body-segments"))));
        Long wristId = catalogId(token, "body-segments", "Muñeca");

        Long diagnosisId = created(perform(token, post("/api/patients/" + patientId + "/diagnoses").contentType(JSON)
                .content(json(surgicalDiagnosis(surgeryTypeId))))).get("id").asLong();
        Long templateId = created(perform(token, post("/api/templates").contentType(JSON)
                .content(json(Map.of("name", "Fractura de muñeca", "segmentId", wristId))))).get("id").asLong();
        Long appointmentId = created(perform(token, post("/api/appointments").contentType(JSON)
                .content(json(appointment(therapistId, patientId, appointmentDate().toString(), "10:00")))))
                .get("id").asLong();
        Long followUpId = created(perform(token, post("/api/diagnoses/" + diagnosisId + "/follow-ups")
                .contentType(JSON).content(json(followUp(scarTypeId, wristId))))).get("id").asLong();

        return new Fixture(establishmentId, token, adminId, therapistId, patientId, surgeryTypeId, scarTypeId,
                segmentIds, wristId, diagnosisId, templateId, appointmentId, followUpId);
    }

    private Long catalogId(String token, String slug, String name) throws Exception {
        for (JsonNode item : read(perform(token, get("/api/catalogs/" + slug)))) {
            if (name.equals(item.get("name").asText())) {
                return item.get("id").asLong();
            }
        }
        throw new AssertionError("Missing default catalog item " + name + " in " + slug);
    }

    /** A Monday at least a week ahead, inside the agenda hours whatever day the suite runs. */
    private LocalDate appointmentDate() {
        return LocalDate.now(clock).plusWeeks(1).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    private <T> T inSession(Function<EntityManager, T> work) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            return work.apply(entityManager);
        } finally {
            entityManager.close();
        }
    }

    private static Map<String, Object> patient(String dni) {
        Map<String, Object> body = new HashMap<>();
        body.put("dni", dni);
        body.put("firstName", "Lucía");
        body.put("lastName", "Fernández");
        body.put("birthDate", "1984-03-21");
        body.put("phone", "249 412-3456");
        body.put("occupation", "Docente");
        body.put("healthInsurance", "OSDE");
        body.put("healthInsuranceNumber", "61 234567 8 01");
        body.put("dominance", "RIGHT");
        return body;
    }

    private static Map<String, Object> surgicalDiagnosis(Long surgeryTypeId) {
        Map<String, Object> treatment = new HashMap<>();
        treatment.put("type", "SURGICAL");
        treatment.put("surgeryDate", "2026-10-14");
        treatment.put("stitchesRemovalDate", "2026-10-28");
        treatment.put("surgeryTypeId", surgeryTypeId);
        return Map.of("description", "Fractura distal de radio", "treatment", treatment);
    }

    private Map<String, Object> followUp(Long scarTypeId, Long segmentId) {
        Map<String, Object> evaluation = new HashMap<>();
        evaluation.put("segmentId", segmentId);
        evaluation.put("pain", 3);
        evaluation.put("rom", 55);
        evaluation.put("strength", 14);
        Map<String, Object> body = new HashMap<>();
        body.put("date", LocalDate.now(clock).toString());
        body.put("scarTypeId", scarTypeId);
        body.put("evaluations", List.of(evaluation));
        return body;
    }

    private static Map<String, Object> appointment(Long therapistId, Long patientId, String date, String time) {
        Map<String, Object> body = new HashMap<>();
        body.put("therapistId", therapistId);
        body.put("patientId", patientId);
        body.put("date", date);
        body.put("startTime", time);
        body.put("durationMinutes", 45);
        return body;
    }

    private ResultActions perform(String token, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private static void expectError(ResultActions result, ErrorCode code) throws Exception {
        result.andExpect(status().is(code.getHttpStatus().value()))
                .andExpect(jsonPath("$.code").value(code.name()));
    }

    private JsonNode created(ResultActions result) throws Exception {
        return body(result.andExpect(status().isCreated()).andReturn());
    }

    private JsonNode read(ResultActions result) throws Exception {
        return body(result.andExpect(status().isOk()).andReturn());
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private static List<Long> ids(JsonNode array) {
        List<Long> ids = new ArrayList<>();
        array.forEach(item -> ids.add(item.get("id").asLong()));
        return ids;
    }

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }
}
