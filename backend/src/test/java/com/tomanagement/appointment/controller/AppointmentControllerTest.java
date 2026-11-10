package com.tomanagement.appointment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomanagement.TestTable;
import com.tomanagement.TestTables;
import com.tomanagement.TestTenancy;
import com.tomanagement.TestcontainersConfiguration;
import com.tomanagement.appointment.repository.AppointmentRepository;
import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.diagnosis.entity.ConservativeTreatment;
import com.tomanagement.diagnosis.entity.Diagnosis;
import com.tomanagement.diagnosis.repository.DiagnosisRepository;
import com.tomanagement.patient.entity.Dominance;
import com.tomanagement.patient.entity.Patient;
import com.tomanagement.patient.entity.PatientData;
import com.tomanagement.patient.repository.PatientRepository;
import com.tomanagement.support.MutableClock;
import com.tomanagement.user.entity.Role;
import com.tomanagement.user.entity.User;
import com.tomanagement.user.repository.UserRepository;

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
 * Agenda appointments (É7) against a real Postgres; "now" is fixed at 2026-11-05 11:40 (Buenos Aires).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestTables.class, TestTenancy.class})
class AppointmentControllerTest {

    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return new MutableClock(Instant.parse("2026-11-05T14:40:00Z")) {
                @Override
                public ZoneId getZone() {
                    return ZoneId.of("America/Argentina/Buenos_Aires");
                }
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestTables testTables;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private TestTenancy tenancy;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Clock clock;

    private Long establishmentId;
    private User admin;
    private User carla;
    private User paula;
    private AuthenticatedUser adminActor;
    private AuthenticatedUser carlaActor;
    private AuthenticatedUser paulaActor;
    private Patient lucia;
    private Patient diego;
    private Diagnosis luciasDiagnosis;

    @BeforeEach
    void seed() {
        testTables.truncate(TestTable.APPOINTMENT, TestTable.FOLLOW_UP, TestTable.DIAGNOSIS, TestTable.TREATMENT,
                TestTable.PATIENT, TestTable.APP_USER, TestTable.ESTABLISHMENT);
        establishmentId = tenancy.create("Consultorio").getId();
        admin = user("admin@consultorio.com", "Admin", Role.ADMIN);
        carla = user("carla@consultorio.com", "Carla Ríos", Role.TERAPEUTA);
        paula = user("paula@consultorio.com", "Paula Gómez", Role.TERAPEUTA);
        adminActor = actor(admin);
        carlaActor = actor(carla);
        paulaActor = actor(paula);
        tenancy.runAs(establishmentId, () -> {
            lucia = patient("30123456", "Lucía", "Fernández");
            diego = patient("28999888", "Diego", "Suárez");
            luciasDiagnosis = diagnosisRepository.save(new Diagnosis(lucia, "Esguince", LocalDate.of(2026, 10, 12),
                    new ConservativeTreatment(true, LocalDate.of(2026, 10, 1))));
        });
    }

    @Test
    void adminBooksForATherapistAndEveryoneSeesWhatTheyShould() throws Exception {
        book(adminActor, carla, lucia, "2026-11-10", "12:00", 45);
        performAs(adminActor, get("/api/appointments?from=2026-11-09&to=2026-11-15"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].endTime").value("12:45:00"))
                .andExpect(jsonPath("$[0].therapist.fullName").value("Carla Ríos"));
        performAs(carlaActor, get("/api/appointments?from=2026-11-09&to=2026-11-15"))
                .andExpect(jsonPath("$.length()").value(1));
        performAs(paulaActor, get("/api/appointments?from=2026-11-09&to=2026-11-15&therapistIds=" + carla.getId()))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void aTherapistCannotBookRescheduleOrCancelForSomeoneElse() throws Exception {
        performAs(paulaActor, post("/api/appointments").contentType(JSON)
                .content(json(create(carla, lucia, null, "2026-11-10", "12:00", 45))))
                .andExpect(status().isForbidden());
        long id = book(adminActor, carla, lucia, "2026-11-10", "12:00", 45);
        performAs(paulaActor, delete("/api/appointments/" + id)).andExpect(status().isForbidden());
        performAs(paulaActor, put("/api/appointments/" + id).contentType(JSON)
                .content(json(reschedule(paula, "2026-11-10", "13:00", 45))))
                .andExpect(status().isForbidden());
    }

    @Test
    void overlappingTherapistOrPatientIsAConflict() throws Exception {
        book(adminActor, carla, lucia, "2026-11-10", "12:00", 60);
        expectError(adminActor, create(carla, diego, null, "2026-11-10", "12:30", 30), 409, ErrorCode.THERAPIST_BUSY);
        expectError(adminActor, create(paula, lucia, null, "2026-11-10", "12:30", 30), 409, ErrorCode.PATIENT_BUSY);
        book(adminActor, carla, diego, "2026-11-10", "13:00", 30); // back to back is fine
    }

    @Test
    void getRespectsVisibility() throws Exception {
        long id = book(adminActor, carla, lucia, "2026-11-10", "12:00", 45);
        performAs(carlaActor, get("/api/appointments/" + id)).andExpect(status().isOk());
        performAs(paulaActor, get("/api/appointments/" + id)).andExpect(status().isForbidden());
        performAs(adminActor, get("/api/appointments/999999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.APPOINTMENT_NOT_FOUND.name()));
    }

    @Test
    void rescheduleKeepsThePatientAndMayOverlapItsOwnSlot() throws Exception {
        long id = book(adminActor, carla, lucia, "2026-11-10", "12:00", 60);
        performAs(carlaActor, put("/api/appointments/" + id).contentType(JSON)
                .content(json(reschedule(carla, "2026-11-10", "12:30", 60))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime").value("12:30:00"))
                .andExpect(jsonPath("$.patient.lastName").value("Fernández"));
    }

    @Test
    void cancelDeletes() throws Exception {
        long id = book(adminActor, carla, lucia, "2026-11-10", "12:00", 45);
        performAs(carlaActor, delete("/api/appointments/" + id)).andExpect(status().isNoContent());
        assertThat(tenancy.callAs(establishmentId, () -> appointmentRepository.existsById(id))).isFalse();
    }

    @Test
    void confirmationWithoutDiagnosisPersistsTheClockInstantAndIsIdempotent() throws Exception {
        long id = book(adminActor, carla, lucia, "2026-11-10", "12:00", 45);
        String confirmedAt = clock.instant().toString();
        performAs(carlaActor, post("/api/appointments/" + id + "/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedAt").value(confirmedAt))
                .andExpect(jsonPath("$.diagnosis").isEmpty())
                .andExpect(jsonPath("$.billed").value(false));
        ((MutableClock) clock).advance(Duration.ofMinutes(5));
        performAs(adminActor, post("/api/appointments/" + id + "/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedAt").value(confirmedAt));
        performAs(carlaActor, get("/api/appointments/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedAt").value(confirmedAt));
    }

    @Test
    void anotherTherapistCannotConfirmAndAnotherTenantSeesANonexistentAppointment() throws Exception {
        long id = book(adminActor, carla, lucia, "2026-11-10", "12:00", 45);
        performAs(paulaActor, post("/api/appointments/" + id + "/confirm"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.name()));
        Long otherEstablishment = tenancy.create("Otro consultorio").getId();
        User otherAdmin = userRepository.save(User.builder().email("other@consultorio.com")
                .passwordHash("x".repeat(60)).fullName("Otro admin").role(Role.ADMIN).active(true)
                .establishmentId(otherEstablishment).createdAt(clock.instant()).build());
        AuthenticatedUser otherActor = new AuthenticatedUser(otherAdmin.getId(), otherAdmin.getEmail(),
                otherAdmin.getFullName(), Role.ADMIN, otherEstablishment);
        performAs(otherActor, post("/api/appointments/" + id + "/confirm"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.APPOINTMENT_NOT_FOUND.name()));
        performAs(carlaActor, get("/api/appointments/" + id)).andExpect(jsonPath("$.confirmedAt").isEmpty());
    }

    @Test
    void confirmedAppointmentsCannotBeCancelledOrRescheduledAndPermissionsAreCheckedFirst() throws Exception {
        long id = book(adminActor, carla, lucia, "2026-11-10", "12:00", 45);
        performAs(carlaActor, post("/api/appointments/" + id + "/confirm")).andExpect(status().isOk());
        performAs(paulaActor, delete("/api/appointments/" + id)).andExpect(status().isForbidden());
        performAs(paulaActor, put("/api/appointments/" + id).contentType(JSON)
                .content(json(reschedule(paula, "2026-11-10", "13:00", 45))))
                .andExpect(status().isForbidden());
        performAs(adminActor, delete("/api/appointments/" + id))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_ALREADY_CONFIRMED"));
        performAs(carlaActor, put("/api/appointments/" + id).contentType(JSON)
                .content(json(reschedule(carla, "2026-11-10", "13:00", 60))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_ALREADY_CONFIRMED"));
        performAs(carlaActor, get("/api/appointments/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime").value("12:00:00"))
                .andExpect(jsonPath("$.durationMinutes").value(45));
    }

    @Test
    void rulesAndRanges() throws Exception {
        expectError(adminActor, create(carla, lucia, null, "2026-11-08", "10:00", 30), 422,
                ErrorCode.APPOINTMENT_OUTSIDE_HOURS);
        expectError(adminActor, create(carla, lucia, null, "2026-11-10", "19:30", 60), 422,
                ErrorCode.APPOINTMENT_OUTSIDE_HOURS);
        expectError(adminActor, create(carla, lucia, null, "2026-11-05", "11:00", 30), 422,
                ErrorCode.APPOINTMENT_IN_PAST);
        expectError(adminActor, create(carla, lucia, null, "2026-11-10", "10:00", 40), 422,
                ErrorCode.APPOINTMENT_DURATION_INVALID);
        expectError(adminActor, create(carla, diego, luciasDiagnosis.getId(), "2026-11-10", "10:00", 30), 422,
                ErrorCode.DIAGNOSIS_NOT_OF_PATIENT);
        performAs(adminActor, get("/api/appointments?from=2026-11-01&to=2026-12-31"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCode.APPOINTMENT_RANGE_TOO_LONG.name()));
    }

    @Test
    void availabilityAndTherapists() throws Exception {
        book(adminActor, carla, lucia, "2026-11-10", "12:00", 45);
        performAs(carlaActor, get("/api/appointments/availability?therapistId=" + carla.getId()
                + "&date=2026-11-10&durationMinutes=45"))
                .andExpect(jsonPath("$[?(@.startTime == '12:00:00')].available").value(false));
        performAs(carlaActor, get("/api/agenda/therapists")).andExpect(jsonPath("$.length()").value(1));
        performAs(adminActor, get("/api/agenda/therapists")).andExpect(jsonPath("$.length()").value(3));
    }

    private long book(AuthenticatedUser actor, User therapist, Patient patient, String date, String time, int minutes)
            throws Exception {
        String body = performAs(actor, post("/api/appointments").contentType(JSON)
                .content(json(create(therapist, patient, null, date, time, minutes))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private void expectError(AuthenticatedUser actor, Map<String, Object> body, int httpStatus, ErrorCode code)
            throws Exception {
        performAs(actor, post("/api/appointments").contentType(JSON).content(json(body)))
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.code").value(code.name()));
    }

    private static Map<String, Object> create(User therapist, Patient patient, Long diagnosisId, String date,
            String time, int minutes) {
        Map<String, Object> body = new HashMap<>();
        body.put("therapistId", therapist.getId());
        body.put("patientId", patient.getId());
        body.put("diagnosisId", diagnosisId);
        body.put("date", date);
        body.put("startTime", time);
        body.put("durationMinutes", minutes);
        return body;
    }

    private static Map<String, Object> reschedule(User therapist, String date, String time, int minutes) {
        Map<String, Object> body = new HashMap<>();
        body.put("therapistId", therapist.getId());
        body.put("date", date);
        body.put("startTime", time);
        body.put("durationMinutes", minutes);
        return body;
    }

    private ResultActions performAs(AuthenticatedUser actor, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tenancy.tokenFor(actor)));
    }

    private String json(Map<String, ?> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private User user(String email, String fullName, Role role) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash("x".repeat(60))
                .fullName(fullName)
                .role(role)
                .active(true)
                .establishmentId(establishmentId)
                .createdAt(Instant.parse("2026-11-04T12:00:00Z"))
                .build());
    }

    private AuthenticatedUser actor(User user) {
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), establishmentId);
    }

    private Patient patient(String dni, String firstName, String lastName) {
        return patientRepository.save(Patient.builder()
                .dni(dni)
                .data(new PatientData(firstName, lastName, LocalDate.of(1984, 3, 21), "249 412-3456", "Docente", "OSDE",
                        "61 234567 8 01", Dominance.RIGHT))
                .createdAt(Instant.parse("2026-11-04T12:00:00Z"))
                .build());
    }
}
