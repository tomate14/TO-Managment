package com.tomanagement.patient.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomanagement.TestTable;
import com.tomanagement.TestTables;
import com.tomanagement.TestTenancy;
import com.tomanagement.TestcontainersConfiguration;
import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.user.entity.Role;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Patients (H4.1–H4.3) against a real Postgres.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestTables.class, TestTenancy.class})
@ExtendWith(OutputCaptureExtension.class)
class PatientControllerTest {

    private static final String PATIENTS = "/api/patients";
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestTables testTables;

    @Autowired
    private TestTenancy tenancy;

    @Autowired
    private ObjectMapper objectMapper;

    private AuthenticatedUser therapist;

    @BeforeEach
    void cleanUp() {
        testTables.truncate(TestTable.PATIENT, TestTable.ESTABLISHMENT);
        Long establishmentId = tenancy.create("Consultorio").getId();
        therapist = new AuthenticatedUser(2L, "t@c.com", "Terapeuta", Role.TERAPEUTA, establishmentId);
    }

    @Test
    void createsAPatientWithTheDniAsDigits() throws Exception {
        perform(post(PATIENTS).contentType(MediaType.APPLICATION_JSON).content(json(patient("30.123.456", "Lucía", "Fernández"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.dni").value("30123456"))
                .andExpect(jsonPath("$.firstName").value("Lucía"))
                .andExpect(jsonPath("$.birthDate").value("1984-03-21"))
                .andExpect(jsonPath("$.dominance").value("RIGHT"));
    }

    @Test
    void duplicateDniIsRejectedWithAClearMessageAndNoDataInTheLogs(CapturedOutput output) throws Exception {
        create("30123456", "Lucía", "Fernández");

        perform(post(PATIENTS).contentType(MediaType.APPLICATION_JSON).content(json(patient("30 123 456", "Otra", "Persona"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.PATIENT_DNI_DUPLICATE.name()))
                .andExpect(jsonPath("$.message").value("Ya existe un paciente con este DNI."));
        assertThat(output.getAll()).doesNotContain("30123456").doesNotContain("Otra");
    }

    @Test
    void everyFieldIsRequiredAndTheBirthDateCannotBeInTheFuture() throws Exception {
        Map<String, Object> invalid = patient("12", " ", "Fernández");
        invalid.put("birthDate", LocalDate.now().plusDays(1).toString());
        invalid.remove("phone");

        perform(post(PATIENTS).contentType(MediaType.APPLICATION_JSON).content(json(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.fieldErrors.length()").value(4));
    }

    @Test
    void searchesByDniNameOrLastNameIgnoringCaseAndOrdersByLastName() throws Exception {
        create("30123456", "Lucía", "Fernández");
        create("22410876", "Ricardo", "Castro");
        create("41276590", "Agustina", "López");

        perform(get(PATIENTS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].lastName").value("Castro"))
                .andExpect(jsonPath("$.content[2].lastName").value("López"))
                .andExpect(jsonPath("$.totalElements").value(3));
        perform(get(PATIENTS).param("q", "FERNÁN")).andExpect(jsonPath("$.content[0].firstName").value("Lucía"));
        perform(get(PATIENTS).param("q", "30.123")).andExpect(jsonPath("$.content[0].lastName").value("Fernández"));
        perform(get(PATIENTS).param("q", "lucía fernández")).andExpect(jsonPath("$.totalElements").value(1));
        perform(get(PATIENTS).param("q", "agustina castro")).andExpect(jsonPath("$.totalElements").value(0));
        perform(get(PATIENTS).param("q", "%")).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void paginatesTheResults() throws Exception {
        create("30000001", "Ana", "Aguirre");
        create("30000002", "Bea", "Benítez");
        create("30000003", "Caro", "Cabrera");

        perform(get(PATIENTS).param("page", "1").param("size", "2"))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].lastName").value("Cabrera"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void editsEverythingButTheDni() throws Exception {
        long id = create("30123456", "Lucía", "Fernández");
        Map<String, Object> changes = patient("99999999", "Lucía Inés", "Fernández");
        changes.remove("dni");
        changes.put("dominance", "LEFT");

        perform(put(PATIENTS + "/" + id).contentType(MediaType.APPLICATION_JSON).content(json(changes)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Lucía Inés"))
                .andExpect(jsonPath("$.dominance").value("LEFT"))
                .andExpect(jsonPath("$.dni").value("30123456"));
        perform(get(PATIENTS + "/" + id)).andExpect(jsonPath("$.dni").value("30123456"));
    }

    @Test
    void unknownPatientIsNotFound() throws Exception {
        perform(get(PATIENTS + "/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.PATIENT_NOT_FOUND.name()));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get(PATIENTS)).andExpect(status().isUnauthorized());
    }

    private long create(String dni, String firstName, String lastName) throws Exception {
        String body = perform(post(PATIENTS).contentType(MediaType.APPLICATION_JSON).content(json(patient(dni, firstName, lastName))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private static Map<String, Object> patient(String dni, String firstName, String lastName) {
        Map<String, Object> body = new HashMap<>();
        body.put("dni", dni);
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        body.put("birthDate", "1984-03-21");
        body.put("phone", "249 412-3456");
        body.put("occupation", "Docente");
        body.put("healthInsurance", "OSDE");
        body.put("healthInsuranceNumber", "61 234567 8 01");
        body.put("dominance", "RIGHT");
        return body;
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tenancy.tokenFor(therapist)));
    }

    private String json(Map<String, ?> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }
}
