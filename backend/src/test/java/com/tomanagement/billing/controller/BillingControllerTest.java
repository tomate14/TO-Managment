package com.tomanagement.billing.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomanagement.TestTable;
import com.tomanagement.TestTables;
import com.tomanagement.TestTenancy;
import com.tomanagement.TestcontainersConfiguration;
import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.diagnosis.entity.ConservativeTreatment;
import com.tomanagement.diagnosis.entity.Diagnosis;
import com.tomanagement.diagnosis.repository.DiagnosisRepository;
import com.tomanagement.followup.entity.FollowUp;
import com.tomanagement.followup.repository.FollowUpRepository;
import com.tomanagement.patient.entity.Dominance;
import com.tomanagement.patient.entity.Patient;
import com.tomanagement.patient.entity.PatientData;
import com.tomanagement.patient.repository.PatientRepository;
import com.tomanagement.user.entity.Role;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Session billing per diagnosis (É12) against a real Postgres; "now" is fixed at 2026-11-08 23:30 (Buenos Aires),
 * which is already 2026-11-09 in UTC.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestTables.class, TestTenancy.class})
class BillingControllerTest {

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-11-09T02:30:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestTables testTables;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private FollowUpRepository followUpRepository;

    @Autowired
    private TestTenancy tenancy;

    @Autowired
    private ObjectMapper objectMapper;

    private AuthenticatedUser therapist;
    private Diagnosis diagnosis;
    /** The 4 follow-ups of the diagnosis, oldest first. */
    private List<Long> sessionIds;
    private Long otherDiagnosisSessionId;

    @BeforeEach
    void seed() {
        testTables.truncate(TestTable.FOLLOW_UP, TestTable.DIAGNOSIS, TestTable.TREATMENT, TestTable.PATIENT,
                TestTable.ESTABLISHMENT);
        Long establishmentId = tenancy.create("Consultorio").getId();
        therapist = new AuthenticatedUser(2L, "t@c.com", "Terapeuta", Role.TERAPEUTA, establishmentId);
        tenancy.runAs(establishmentId, this::seedData);
    }

    private void seedData() {
        Patient lucia = patientRepository.save(Patient.builder()
                .dni("30123456")
                .data(new PatientData("Lucía", "Fernández", LocalDate.of(1984, 3, 21), "249 412-3456", "Docente", "OSDE",
                        "61 234567 8 01", Dominance.RIGHT))
                .createdAt(Instant.parse("2026-11-04T12:00:00Z"))
                .build());
        diagnosis = diagnosisRepository.save(new Diagnosis(lucia, "Esguince", LocalDate.of(2026, 10, 12),
                new ConservativeTreatment(true, LocalDate.of(2026, 10, 1))));
        Diagnosis other = diagnosisRepository.save(new Diagnosis(lucia, "Fractura", LocalDate.of(2026, 10, 12),
                new ConservativeTreatment(false, LocalDate.of(2026, 10, 1))));
        // Saved out of order to check that the summary sorts by date.
        Long third = followUp(diagnosis, "2026-10-20");
        Long first = followUp(diagnosis, "2026-10-06");
        Long fourth = followUp(diagnosis, "2026-10-27");
        Long second = followUp(diagnosis, "2026-10-13");
        sessionIds = List.of(first, second, third, fourth);
        otherDiagnosisSessionId = followUp(other, "2026-10-15");
    }

    private Long followUp(Diagnosis of, String date) {
        FollowUp saved = followUpRepository.save(FollowUp.builder().diagnosis(of).date(LocalDate.parse(date)).build());
        return saved.getId();
    }

    @Test
    void summaryCountsSessions() throws Exception {
        perform(get(billing()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSessions").value(4))
                .andExpect(jsonPath("$.billedSessions").value(0))
                .andExpect(jsonPath("$.pendingSessions").value(4))
                .andExpect(jsonPath("$.pending.length()").value(4))
                .andExpect(jsonPath("$.pending[0].followUpId").value(sessionIds.get(0)))
                .andExpect(jsonPath("$.pending[0].date").value("2026-10-06"))
                .andExpect(jsonPath("$.pending[0].segments.length()").value(0))
                .andExpect(jsonPath("$.pending[1].date").value("2026-10-13"))
                .andExpect(jsonPath("$.pending[2].date").value("2026-10-20"))
                .andExpect(jsonPath("$.pending[3].date").value("2026-10-27"))
                .andExpect(jsonPath("$.billings.length()").value(0));
    }

    @Test
    void registersABillingOfPendingSessions() throws Exception {
        perform(post(billings()).contentType(MediaType.APPLICATION_JSON)
                .content(json(request("2026-11-08", 25000.50, sessionIds.subList(0, 3)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalSessions").value(4))
                .andExpect(jsonPath("$.billedSessions").value(3))
                .andExpect(jsonPath("$.pendingSessions").value(1))
                .andExpect(jsonPath("$.pending[0].followUpId").value(sessionIds.get(3)))
                .andExpect(jsonPath("$.billings.length()").value(1))
                .andExpect(jsonPath("$.billings[0].sessionCount").value(3))
                .andExpect(jsonPath("$.billings[0].amount").value(25000.5))
                .andExpect(jsonPath("$.billings[0].billedOn").value("2026-11-08"))
                .andExpect(jsonPath("$.billings[0].voided").value(false))
                .andExpect(jsonPath("$.billings[0].sessions.length()").value(3))
                .andExpect(jsonPath("$.billings[0].sessions[0].date").value("2026-10-06"));
    }

    @Test
    void needsAtLeastOneSession() throws Exception {
        expectError(request("2026-11-08", 1000, List.of()), ErrorCode.BILLING_SESSIONS_REQUIRED);
    }

    @Test
    void amountMustBePositive() throws Exception {
        expectError(request("2026-11-08", 0, sessionIds), ErrorCode.BILLING_AMOUNT_INVALID);

        Map<String, Object> noAmount = request("2026-11-08", 0, sessionIds);
        noAmount.put("amount", null);
        perform(post(billings()).contentType(MediaType.APPLICATION_JSON).content(json(noAmount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()));
    }

    @Test
    void dateCannotBeInTheFuture() throws Exception {
        perform(post(billings()).contentType(MediaType.APPLICATION_JSON)
                .content(json(request("2026-11-08", 1000, sessionIds.subList(0, 1)))))
                .andExpect(status().isCreated());
        expectError(request("2026-11-09", 1000, sessionIds.subList(1, 2)), ErrorCode.BILLING_DATE_IN_FUTURE);
    }

    @Test
    void rejectsSessionsOfAnotherDiagnosisRepeatedOrAlreadyBilled() throws Exception {
        expectError(request("2026-11-08", 1000, List.of(sessionIds.get(0), otherDiagnosisSessionId)),
                ErrorCode.BILLING_SESSION_INVALID);
        expectError(request("2026-11-08", 1000, List.of(sessionIds.get(0), sessionIds.get(0))),
                ErrorCode.BILLING_SESSION_INVALID);

        perform(post(billings()).contentType(MediaType.APPLICATION_JSON)
                .content(json(request("2026-11-08", 1000, sessionIds.subList(0, 2)))))
                .andExpect(status().isCreated());
        expectError(request("2026-11-08", 1000, List.of(sessionIds.get(1), sessionIds.get(2))),
                ErrorCode.BILLING_SESSION_INVALID);
    }

    @Test
    void voidingReturnsSessionsToPending() throws Exception {
        Long billingId = register(sessionIds.subList(0, 3));

        perform(post("/api/billings/" + billingId + "/void"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingSessions").value(4))
                .andExpect(jsonPath("$.billedSessions").value(0))
                .andExpect(jsonPath("$.billings[0].voided").value(true))
                .andExpect(jsonPath("$.billings[0].voidedAt").isNotEmpty())
                .andExpect(jsonPath("$.billings[0].sessionCount").value(3));

        perform(post("/api/billings/" + billingId + "/void"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingSessions").value(4))
                .andExpect(jsonPath("$.billings[0].voided").value(true));
    }

    @Test
    void aSessionOfAVoidedBillingCanBeBilledAgain() throws Exception {
        Long billingId = register(sessionIds.subList(0, 2));
        perform(post("/api/billings/" + billingId + "/void")).andExpect(status().isOk());

        perform(post(billings()).contentType(MediaType.APPLICATION_JSON)
                .content(json(request("2026-11-08", 3000, sessionIds.subList(0, 2)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.billedSessions").value(2))
                .andExpect(jsonPath("$.pendingSessions").value(2))
                .andExpect(jsonPath("$.billings.length()").value(2));
    }

    @Test
    void unknownBillingIsNotFound() throws Exception {
        perform(post("/api/billings/999999/void"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.BILLING_NOT_FOUND.name()));
    }

    @Test
    void unknownDiagnosisIsNotFound() throws Exception {
        perform(get("/api/diagnoses/999999/billing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.DIAGNOSIS_NOT_FOUND.name()));
    }

    private Long register(List<Long> followUpIds) throws Exception {
        String body = perform(post(billings()).contentType(MediaType.APPLICATION_JSON)
                .content(json(request("2026-11-08", 25000.50, followUpIds))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode summary = objectMapper.readTree(body);
        return summary.get("billings").get(0).get("id").asLong();
    }

    private void expectError(Map<String, Object> body, ErrorCode code) throws Exception {
        perform(post(billings()).contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().is(code.getHttpStatus().value()))
                .andExpect(jsonPath("$.code").value(code.name()));
    }

    private String billing() {
        return "/api/diagnoses/" + diagnosis.getId() + "/billing";
    }

    private String billings() {
        return "/api/diagnoses/" + diagnosis.getId() + "/billings";
    }

    private static Map<String, Object> request(String billedOn, Number amount, List<Long> followUpIds) {
        Map<String, Object> body = new HashMap<>();
        body.put("billedOn", billedOn);
        body.put("amount", amount);
        body.put("followUpIds", followUpIds);
        return body;
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tenancy.tokenFor(therapist)));
    }

    private String json(Map<String, ?> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }
}
