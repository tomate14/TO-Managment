package com.tomanagement.billing.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomanagement.TestTable;
import com.tomanagement.TestTables;
import com.tomanagement.TestTenancy;
import com.tomanagement.TestcontainersConfiguration;
import com.tomanagement.appointment.entity.Appointment;
import com.tomanagement.appointment.entity.AppointmentDuration;
import com.tomanagement.appointment.repository.AppointmentRepository;
import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.billing.dto.CreateBillingRequest;
import com.tomanagement.billing.entity.Billing;
import com.tomanagement.billing.repository.BillingRepository;
import com.tomanagement.billing.service.BillingService;
import com.tomanagement.catalog.entity.BodySegment;
import com.tomanagement.catalog.repository.BodySegmentRepository;
import com.tomanagement.common.exception.ApplicationException;
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
import com.tomanagement.user.entity.User;
import com.tomanagement.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Patient appointment billing against a real Postgres; "now" is fixed at 2026-11-08 23:30 (Buenos Aires),
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
    private BodySegmentRepository bodySegmentRepository;

    @Autowired
    private TestTenancy tenancy;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BillingService billingService;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Value("${APP_DB_SCHEMA}")
    private String schema;
    private AuthenticatedUser therapist;
    private Diagnosis diagnosis;
    private Patient patient;
    private User user;
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BillingRepository billingRepository;
    private Long unconfirmedId;
    private Long otherPatientId;
    /** Confirmed appointments across diagnoses, oldest first. */
    private List<Long> sessionIds;
    private Long otherDiagnosisSessionId;

    @BeforeEach
    void seed() {
        testTables.truncate(TestTable.APPOINTMENT, TestTable.APP_USER, TestTable.FOLLOW_UP, TestTable.DIAGNOSIS, TestTable.TREATMENT, TestTable.PATIENT,
                TestTable.BODY_SEGMENT, TestTable.ESTABLISHMENT);
        Long establishmentId = tenancy.create("Consultorio").getId();
        user = userRepository.save(User.builder().email("t@c.com").passwordHash("unused").fullName("Terapeuta")
                .role(Role.TERAPEUTA).establishmentId(establishmentId).createdAt(Instant.parse("2026-11-01T12:00:00Z")).build());
        therapist = new AuthenticatedUser(user.getId(), "t@c.com", "Terapeuta", Role.TERAPEUTA, establishmentId);
        tenancy.runAs(establishmentId, this::seedData);
    }

    private void seedData() {
        Patient lucia = patientRepository.save(Patient.builder()
                .dni("30123456")
                .data(new PatientData("Lucía", "Fernández", LocalDate.of(1984, 3, 21), "249 412-3456", "Docente", "OSDE",
                        "61 234567 8 01", Dominance.RIGHT))
                .createdAt(Instant.parse("2026-11-04T12:00:00Z"))
                .build());
        patient = lucia;
        diagnosis = diagnosisRepository.save(new Diagnosis(lucia, "Esguince", LocalDate.of(2026, 10, 12),
                new ConservativeTreatment(true, LocalDate.of(2026, 10, 1))));
        Diagnosis other = diagnosisRepository.save(new Diagnosis(lucia, "Fractura", LocalDate.of(2026, 10, 12),
                new ConservativeTreatment(false, LocalDate.of(2026, 10, 1))));
        Long third = appointment(lucia, null, "2026-10-20T10:00", true);
        Long first = appointment(lucia, diagnosis, "2026-10-06T10:00", true);
        Long second = appointment(lucia, other, "2026-10-13T10:00", true);
        sessionIds = List.of(first, second, third);
        unconfirmedId = appointment(lucia, diagnosis, "2026-10-27T10:00", false);
        Patient another = patientRepository.save(Patient.builder().dni("30999999").data(new PatientData("Otra", "Paciente", LocalDate.of(1984, 3, 21), "123", "Docente", "OSDE", "123", Dominance.RIGHT))
                .createdAt(Instant.parse("2026-11-01T12:00:00Z")).build());
        otherPatientId = appointment(another, null, "2026-10-28T10:00", true);
        followUp(diagnosis, "2026-10-06");
        otherDiagnosisSessionId = followUp(other, "2026-10-15");
    }

    private Long appointment(Patient of, Diagnosis diagnosis, String start, boolean confirmed) {
        Appointment appointment = new Appointment(user, of, diagnosis, LocalDateTime.parse(start),
                AppointmentDuration.MINUTES_45, null, Instant.parse("2026-10-01T12:00:00Z"));
        if (confirmed) appointment.confirm(Instant.parse("2026-11-01T12:00:00Z"));
        return appointmentRepository.saveAndFlush(appointment).getId();
    }

    private Long followUp(Diagnosis of, String date) {
        FollowUp saved = followUpRepository.save(FollowUp.builder().diagnosis(of).date(LocalDate.parse(date)).build());
        return saved.getId();
    }

    @Test
    void summaryCountsSessions() throws Exception {
        perform(get(billing()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSessions").value(3))
                .andExpect(jsonPath("$.billedSessions").value(0))
                .andExpect(jsonPath("$.pendingSessions").value(3))
                .andExpect(jsonPath("$.pending.length()").value(3))
                .andExpect(jsonPath("$.pending[0].appointmentId").value(sessionIds.get(0)))
                .andExpect(jsonPath("$.pending[0].date").value("2026-10-06"))
                .andExpect(jsonPath("$.pending[0].origin").value("APPOINTMENT"))
                .andExpect(jsonPath("$.pending[0].startTime").value("10:00:00"))
                .andExpect(jsonPath("$.pending[0].confirmedAt").value("2026-11-01T12:00:00Z"))
                .andExpect(jsonPath("$.pending[2].diagnosis").isEmpty())
                .andExpect(jsonPath("$.pending[0].segments.length()").value(0))
                .andExpect(jsonPath("$.pending[1].date").value("2026-10-13"))
                .andExpect(jsonPath("$.pending[2].date").value("2026-10-20"))
                .andExpect(jsonPath("$.billings.length()").value(0));
    }

    @Test
    void savingAClinicalFollowUpDoesNotCreateAPendingSession() throws Exception {
        Long wrist = tenancy.callAs(therapist.establishmentId(),
                () -> bodySegmentRepository.save(new BodySegment("Muñeca")).getId());
        Long hand = tenancy.callAs(therapist.establishmentId(),
                () -> bodySegmentRepository.save(new BodySegment("Mano")).getId());
        // Evaluated hand first, although the wrist segment was created first.
        Map<String, Object> body = new HashMap<>();
        body.put("date", "2026-10-30");
        body.put("evaluations", new ArrayList<>(List.of(evaluationOf(hand), evaluationOf(wrist))));
        perform(post("/api/diagnoses/" + diagnosis.getId() + "/follow-ups").contentType(MediaType.APPLICATION_JSON)
                .content(json(body)))
                .andExpect(status().isCreated());

        perform(get(billing()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending.length()").value(3))
                .andExpect(jsonPath("$.totalSessions").value(3));
    }

    private static Map<String, Object> evaluationOf(Long segmentId) {
        Map<String, Object> evaluation = new HashMap<>();
        evaluation.put("segmentId", segmentId);
        evaluation.put("pain", 2);
        evaluation.put("strength", new BigDecimal("16"));
        evaluation.put("metrics", new ArrayList<>());
        return evaluation;
    }

    @Test
    void registersABillingOfPendingSessions() throws Exception {
        perform(post(billings()).contentType(MediaType.APPLICATION_JSON)
                .content(json(request("2026-11-08", 25000.50, sessionIds.subList(0, 1)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalSessions").value(3))
                .andExpect(jsonPath("$.billedSessions").value(1))
                .andExpect(jsonPath("$.pendingSessions").value(2))
                .andExpect(jsonPath("$.pending[0].appointmentId").value(sessionIds.get(1)))
                .andExpect(jsonPath("$.billings.length()").value(1))
                .andExpect(jsonPath("$.billings[0].sessionCount").value(1))
                .andExpect(jsonPath("$.billings[0].amount").value(25000.5))
                .andExpect(jsonPath("$.billings[0].billedOn").value("2026-11-08"))
                .andExpect(jsonPath("$.billings[0].voided").value(false))
                .andExpect(jsonPath("$.billings[0].sessions.length()").value(1))
                .andExpect(jsonPath("$.billings[0].sessions[0].date").value("2026-10-06"));
    }

    @Test
    void aBillingCanGroupDifferentDiagnosesAndNoDiagnosis() throws Exception {
        perform(post(billings()).contentType(MediaType.APPLICATION_JSON)
                .content(json(request("2026-11-08", 3000, List.of(sessionIds.get(2), sessionIds.get(0), sessionIds.get(1))))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.billedSessions").value(3))
                .andExpect(jsonPath("$.pendingSessions").value(0))
                .andExpect(jsonPath("$.billings[0].sessionCount").value(3))
                .andExpect(jsonPath("$.billings[0].sessions[0].appointmentId").value(sessionIds.get(0)))
                .andExpect(jsonPath("$.billings[0].sessions[1].appointmentId").value(sessionIds.get(1)))
                .andExpect(jsonPath("$.billings[0].sessions[2].appointmentId").value(sessionIds.get(2)))
                .andExpect(jsonPath("$.billings[0].sessions[2].diagnosis").isEmpty());
    }

    @Test
    void needsAtLeastOneSession() throws Exception {
        expectError(request("2026-11-08", 1000, List.of()), ErrorCode.BILLING_SESSIONS_REQUIRED);
    }

    @Test
    void amountMustBePositive() throws Exception {
        expectError(request("2026-11-08", 0, sessionIds), ErrorCode.BILLING_AMOUNT_INVALID);
        expectError(request("2026-11-08", -1, sessionIds), ErrorCode.BILLING_AMOUNT_INVALID);

        Map<String, Object> noAmount = request("2026-11-08", 0, sessionIds);
        noAmount.put("amount", null);
        perform(post(billings()).contentType(MediaType.APPLICATION_JSON).content(json(noAmount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()));
    }

    @Test
    void amountMustFitTheColumn() throws Exception {
        for (String amount : List.of("12345678901.00", "1.234")) {
            Map<String, Object> body = request("2026-11-08", 0, sessionIds);
            body.put("amount", new BigDecimal(amount));
            perform(post(billings()).contentType(MediaType.APPLICATION_JSON).content(json(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()));
        }
    }

    @Test
    void dateCannotBeInTheFuture() throws Exception {
        perform(post(billings()).contentType(MediaType.APPLICATION_JSON)
                .content(json(request("2026-11-08", 1000, sessionIds.subList(0, 1)))))
                .andExpect(status().isCreated());
        expectError(request("2026-11-09", 1000, sessionIds.subList(1, 2)), ErrorCode.BILLING_DATE_IN_FUTURE);
    }

    @Test
    void rejectsUnconfirmedForeignPatientRepeatedAndAlreadyBilledSessions() throws Exception {
        expectError(request("2026-11-08", 1000, List.of(sessionIds.get(0), otherPatientId)),
                ErrorCode.BILLING_SESSION_INVALID);
        expectError(request("2026-11-08", 1000, List.of(unconfirmedId)), ErrorCode.BILLING_SESSION_INVALID);
        expectError(request("2026-11-08", 1000, List.of(999999L)), ErrorCode.BILLING_SESSION_INVALID);
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
        Long billingId = register(sessionIds.subList(0, 1));

        perform(post("/api/billings/" + billingId + "/void"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingSessions").value(3))
                .andExpect(jsonPath("$.billedSessions").value(0))
                .andExpect(jsonPath("$.billings[0].voided").value(true))
                .andExpect(jsonPath("$.billings[0].voidedAt").isNotEmpty())
                .andExpect(jsonPath("$.billings[0].sessionCount").value(1));

        perform(post("/api/billings/" + billingId + "/void"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingSessions").value(3))
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
                .andExpect(jsonPath("$.pendingSessions").value(1))
                .andExpect(jsonPath("$.billings.length()").value(2));
    }

    @Test
    void unknownBillingIsNotFound() throws Exception {
        perform(post("/api/billings/999999/void"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.BILLING_NOT_FOUND.name()));
    }

    @Test
    void unknownPatientIsNotFound() throws Exception {
        perform(get("/api/patients/999999/billing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.PATIENT_NOT_FOUND.name()));
    }

    @Test
    void oldDiagnosisRoutesAreRemoved() throws Exception {
        perform(get("/api/diagnoses/" + diagnosis.getId() + "/billing")).andExpect(status().isNotFound());
        perform(post("/api/diagnoses/" + diagnosis.getId() + "/billings").contentType(MediaType.APPLICATION_JSON)
                .content(json(request("2026-11-08", 1000, sessionIds)))).andExpect(status().isNotFound());
    }

    @Test
    void omittedSelectionAndEmptyPayloadAreRejected() throws Exception {
        Map<String, Object> request = request("2026-11-08", 1000, sessionIds);
        request.remove("appointmentIds");
        expectError(request, ErrorCode.BILLING_SESSIONS_REQUIRED);
        perform(post(billings()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mixedHistoryKeepsLegacySessionsWhenVoidedWithoutAddingPending() throws Exception {
        Long legacy = tenancy.callAs(therapist.establishmentId(), () -> new TransactionTemplate(transactionManager).execute(transaction -> {
            FollowUp followUp = followUpRepository.findById(otherDiagnosisSessionId).orElseThrow();
            return billingRepository.saveAndFlush(new Billing(followUp.getDiagnosis(), LocalDate.of(2026, 10, 30),
                    new BigDecimal("1000"), Instant.parse("2026-11-01T12:00:00Z"), List.of(followUp))).getId();
        }));
        register(sessionIds.subList(0, 1));
        perform(get(billing())).andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingSessions").value(2))
                .andExpect(jsonPath("$.billings[0].origin").value("APPOINTMENT"))
                .andExpect(jsonPath("$.billings[1].origin").value("LEGACY_FOLLOW_UP"))
                .andExpect(jsonPath("$.billings[1].sessions[0].followUpId").value(otherDiagnosisSessionId));
        perform(post("/api/billings/" + legacy + "/void")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSessions").value(3))
                .andExpect(jsonPath("$.pendingSessions").value(2))
                .andExpect(jsonPath("$.billings[1].sessionCount").value(1));
        perform(get("/api/diagnoses/" + diagnosis.getId() + "/follow-ups"))
                .andExpect(status().isOk());
    }

    @Test
    void agendaReflectsActiveBillingAndVoid() throws Exception {
        Long id = register(sessionIds.subList(0, 1));
        perform(get("/api/appointments/" + sessionIds.get(0))).andExpect(status().isOk())
                .andExpect(jsonPath("$.billed").value(true));
        perform(get("/api/appointments").param("from", "2026-10-06").param("to", "2026-10-06"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].billed").value(true));
        perform(post("/api/billings/" + id + "/void")).andExpect(status().isOk());
        perform(get("/api/appointments/" + sessionIds.get(0))).andExpect(status().isOk())
                .andExpect(jsonPath("$.billed").value(false));
    }

    @Test
    void concurrentRegistrationsHaveOnlyOneSuccessfulActiveBilling() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<String> register = () -> tenancy.callAs(therapist.establishmentId(), () -> {
                try {
                    new TransactionTemplate(transactionManager).executeWithoutResult(transaction -> {
                        try {
                            barrier.await(10, TimeUnit.SECONDS);
                        } catch (Exception exception) {
                            throw new AssertionError(exception);
                        }
                        billingService.register(patient.getId(), new CreateBillingRequest(LocalDate.of(2026, 11, 8),
                                new BigDecimal("1000"), sessionIds.subList(0, 1)));
                    });
                    return "CREATED";
                } catch (ApplicationException exception) {
                    return exception.getErrorCode().name();
                }
            });
            var first = executor.submit(register);
            var second = executor.submit(register);
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("CREATED", "BILLING_SESSION_INVALID");
        }
        assertThat(jdbcTemplate.queryForObject("select count(*) from " + schema + ".billing_appointment where active and establishment_id = ?",
                Long.class, therapist.establishmentId())).isEqualTo(1L);
        perform(get(billing())).andExpect(status().isOk())
                .andExpect(jsonPath("$.billings.length()").value(1))
                .andExpect(jsonPath("$.billedSessions").value(1))
                .andExpect(jsonPath("$.pendingSessions").value(2));
    }

    private Long register(List<Long> appointmentIds) throws Exception {
        String body = perform(post(billings()).contentType(MediaType.APPLICATION_JSON)
                .content(json(request("2026-11-08", 25000.50, appointmentIds))))
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
        return "/api/patients/" + patient.getId() + "/billing";
    }

    private String billings() {
        return "/api/patients/" + patient.getId() + "/billings";
    }

    private static Map<String, Object> request(String billedOn, Number amount, List<Long> appointmentIds) {
        Map<String, Object> body = new HashMap<>();
        body.put("billedOn", billedOn);
        body.put("amount", amount);
        body.put("appointmentIds", appointmentIds);
        return body;
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tenancy.tokenFor(therapist)));
    }

    private String json(Map<String, ?> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }
}
