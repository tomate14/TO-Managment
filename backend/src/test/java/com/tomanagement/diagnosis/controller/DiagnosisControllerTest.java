package com.tomanagement.diagnosis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
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
import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.catalog.entity.SurgeryType;
import com.tomanagement.catalog.repository.SurgeryTypeRepository;
import com.tomanagement.common.exception.ErrorCode;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Diagnoses (H5.1, H5.2) against a real Postgres.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestTables.class, TestTenancy.class})
class DiagnosisControllerTest {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestTables testTables;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private SurgeryTypeRepository surgeryTypeRepository;

    @Autowired
    private TestTenancy tenancy;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Clock clock;

    private Patient lucia;
    private SurgeryType plate;
    private SurgeryType retired;
    private AuthenticatedUser therapist;

    @BeforeEach
    void seed() {
        testTables.truncate(TestTable.DIAGNOSIS, TestTable.TREATMENT, TestTable.PATIENT, TestTable.SURGERY_TYPE,
                TestTable.ESTABLISHMENT);
        Long establishmentId = tenancy.create("Consultorio").getId();
        therapist = new AuthenticatedUser(2L, "t@c.com", "Terapeuta", Role.TERAPEUTA, establishmentId);
        tenancy.runAs(establishmentId, () -> {
            lucia = patientRepository.save(Patient.builder()
                    .dni("30123456")
                    .data(new PatientData("Lucía", "Fernández", LocalDate.of(1984, 3, 21), "249 412-3456", "Docente",
                            "OSDE", "61 234567 8 01", Dominance.RIGHT))
                    .createdAt(Instant.parse("2026-11-04T12:00:00Z"))
                    .build());
            plate = surgeryTypeRepository.save(new SurgeryType("Osteosíntesis con placa"));
            SurgeryType arthrodesis = new SurgeryType("Artrodesis");
            arthrodesis.changeActive(false);
            retired = surgeryTypeRepository.save(arthrodesis);
        });
    }

    @Test
    void theClockFollowsTheConfiguredTimeZone() {
        assertThat(clock.getZone()).isEqualTo(ZoneId.of("America/Argentina/Buenos_Aires"));
    }

    @Test
    void createsASurgicalDiagnosisDatedTodayInTheClinicTimeZone() throws Exception {
        perform(post(diagnoses()).contentType(MediaType.APPLICATION_JSON).content(json(surgical("  Fractura distal de radio izquierdo ",
                        "2026-10-14", "2026-10-28", plate.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.description").value("Fractura distal de radio izquierdo"))
                .andExpect(jsonPath("$.createdDate").value(LocalDate.now(clock).toString()))
                .andExpect(jsonPath("$.patient.lastName").value("Fernández"))
                .andExpect(jsonPath("$.treatment.type").value("SURGICAL"))
                .andExpect(jsonPath("$.treatment.surgeryDate").value("2026-10-14"))
                .andExpect(jsonPath("$.treatment.stitchesRemovalDate").value("2026-10-28"))
                .andExpect(jsonPath("$.treatment.surgeryType.name").value("Osteosíntesis con placa"))
                .andExpect(jsonPath("$.treatment.requiresScarType").value(true))
                .andExpect(jsonPath("$.treatment.immobilizationDate").doesNotExist());
    }

    @Test
    void createsAConservativeDiagnosis() throws Exception {
        perform(post(diagnoses()).contentType(MediaType.APPLICATION_JSON).content(json(conservative("Tenosinovitis de De Quervain", "2026-08-02"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.treatment.type").value("CONSERVATIVE"))
                .andExpect(jsonPath("$.treatment.immobilizationDate").value("2026-08-02"))
                .andExpect(jsonPath("$.treatment.requiresScarType").value(false))
                .andExpect(jsonPath("$.treatment.surgeryType").doesNotExist());
    }

    @Test
    void stitchesRemovalBeforeTheSurgeryIsRejected() throws Exception {
        perform(post(diagnoses()).contentType(MediaType.APPLICATION_JSON).content(json(surgical("Fractura", "2026-10-14", "2026-10-13", plate.getId()))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCode.STITCHES_BEFORE_SURGERY.name()));
    }

    @Test
    void inactiveSurgeryTypesAreRejected() throws Exception {
        perform(post(diagnoses()).contentType(MediaType.APPLICATION_JSON).content(json(surgical("Fractura", "2026-10-14", "2026-10-28", retired.getId()))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCode.SURGERY_TYPE_INACTIVE.name()));
    }

    @Test
    void conservativeNeedsTheImmobilizationDate() throws Exception {
        perform(post(diagnoses()).contentType(MediaType.APPLICATION_JSON).content(json(conservative("Esguince", null))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCode.IMMOBILIZATION_DATE_REQUIRED.name()));
    }

    @Test
    void descriptionAndTreatmentTypeAreRequired() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("description", "  ");
        body.put("treatment", Map.of());
        perform(post(diagnoses()).contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }

    @Test
    void listsNewestFirstAndReadsOneById() throws Exception {
        long first = create(conservative("Tenosinovitis de De Quervain", "2026-08-02"));
        long second = create(surgical("Fractura distal de radio izquierdo", "2026-10-14", "2026-10-28", plate.getId()));

        perform(get(diagnoses()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(second))
                .andExpect(jsonPath("$[1].id").value(first));
        perform(get("/api/diagnoses/" + first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Tenosinovitis de De Quervain"))
                .andExpect(jsonPath("$.patient.dni").value("30123456"));
    }

    @Test
    void unknownPatientOrDiagnosisIsNotFound() throws Exception {
        perform(get("/api/patients/999999/diagnoses"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.PATIENT_NOT_FOUND.name()));
        perform(get("/api/diagnoses/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.DIAGNOSIS_NOT_FOUND.name()));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get(diagnoses())).andExpect(status().isUnauthorized());
    }

    private long create(Map<String, Object> body) throws Exception {
        String response = perform(post(diagnoses()).contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private String diagnoses() {
        return "/api/patients/" + lucia.getId() + "/diagnoses";
    }

    private static Map<String, Object> surgical(String description, String surgery, String stitches, Long surgeryTypeId) {
        Map<String, Object> treatment = new HashMap<>();
        treatment.put("type", "SURGICAL");
        treatment.put("surgeryDate", surgery);
        treatment.put("stitchesRemovalDate", stitches);
        treatment.put("surgeryTypeId", surgeryTypeId);
        return Map.of("description", description, "treatment", treatment);
    }

    private static Map<String, Object> conservative(String description, String immobilization) {
        Map<String, Object> treatment = new HashMap<>();
        treatment.put("type", "CONSERVATIVE");
        treatment.put("immobilizationDate", immobilization);
        return Map.of("description", description, "treatment", treatment);
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tenancy.tokenFor(therapist)));
    }

    private String json(Map<String, ?> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }
}
