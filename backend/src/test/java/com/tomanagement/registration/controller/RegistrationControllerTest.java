package com.tomanagement.registration.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomanagement.TestTable;
import com.tomanagement.TestTables;
import com.tomanagement.TestTenancy;
import com.tomanagement.TestcontainersConfiguration;
import com.tomanagement.catalog.repository.BodySegmentRepository;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.establishment.entity.Establishment;
import com.tomanagement.establishment.entity.EstablishmentKind;
import com.tomanagement.establishment.repository.EstablishmentRepository;
import com.tomanagement.user.entity.Role;
import com.tomanagement.user.entity.User;
import com.tomanagement.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestTables.class, TestTenancy.class})
class RegistrationControllerTest {

    private static final String REGISTER = "/api/auth/register";
    private static final String PASSWORD = "secreto123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EstablishmentRepository establishmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BodySegmentRepository bodySegmentRepository;

    @Autowired
    private TestTables testTables;

    @Autowired
    private TestTenancy tenancy;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clean() {
        testTables.truncate(TestTable.APP_USER, TestTable.ESTABLISHMENT);
    }

    @Test
    void registersAPendingIndividualPractice() throws Exception {
        register("Ana Pérez", "ana@gmail.com", PASSWORD).andExpect(status().isCreated());

        List<Establishment> all = establishmentRepository.findAllByOrderByNameAsc();
        assertThat(all).hasSize(1);
        Establishment practice = all.get(0);
        assertThat(practice.getName()).isEqualTo("Ana Pérez");
        assertThat(practice.getKind()).isEqualTo(EstablishmentKind.INDIVIDUAL);
        assertThat(practice.isPendingApproval()).isTrue();

        User admin = userRepository.findByEmail("ana@gmail.com").orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getEstablishmentId()).isEqualTo(practice.getId());
        assertThat(tenancy.callAs(practice.getId(), () -> bodySegmentRepository.findAllByOrderByNameAsc())).hasSize(5);
    }

    @Test
    void normalizesTheEmailAndRejectsDuplicates() throws Exception {
        register("Ana Pérez", "ana@gmail.com", PASSWORD).andExpect(status().isCreated());

        register("Otra Ana", " Ana@Gmail.com ", PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_EMAIL_TAKEN.name()));
    }

    @Test
    void aRepeatedNameGetsASuffix() throws Exception {
        register("Ana Pérez", "ana1@gmail.com", PASSWORD).andExpect(status().isCreated());
        register("Ana Pérez", "ana2@gmail.com", PASSWORD).andExpect(status().isCreated());

        assertThat(establishmentRepository.findAllByOrderByNameAsc())
                .extracting(Establishment::getName)
                .containsExactly("Ana Pérez", "Ana Pérez (2)");
    }

    @Test
    void validatesTheBody() throws Exception {
        register("", "ana@gmail.com", PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("fullName"));

        register("Ana Pérez", "ana@gmail.com", "1234567")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"));
    }

    @Test
    void isReachableWithoutAToken() throws Exception {
        register("Ana Pérez", "ana@gmail.com", PASSWORD)
                .andExpect(status().isCreated());
    }

    private ResultActions register(String fullName, String email, String password) throws Exception {
        return mockMvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("fullName", fullName, "email", email, "password", password))));
    }
}
