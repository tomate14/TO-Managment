package com.tomanagement.segmentmetric.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomanagement.TestTable;
import com.tomanagement.TestTables;
import com.tomanagement.TestTenancy;
import com.tomanagement.TestcontainersConfiguration;
import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.catalog.entity.BodySegment;
import com.tomanagement.catalog.repository.BodySegmentRepository;
import com.tomanagement.common.exception.ErrorCode;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestTables.class, TestTenancy.class})
class SegmentMetricControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BodySegmentRepository bodySegmentRepository;

    @Autowired
    private TestTables testTables;

    @Autowired
    private TestTenancy tenancy;

    @Autowired
    private ObjectMapper objectMapper;

    private Long elbowId;

    private Long shoulderId;

    private AuthenticatedUser admin;

    private AuthenticatedUser therapist;

    @BeforeEach
    void seed() {
        testTables.truncate(TestTable.BODY_SEGMENT, TestTable.ESTABLISHMENT);
        Long establishmentId = tenancy.create("Consultorio").getId();
        admin = new AuthenticatedUser(1L, "admin@c.com", "Admin", Role.ADMIN, establishmentId);
        therapist = new AuthenticatedUser(2L, "t@c.com", "Terapeuta", Role.TERAPEUTA, establishmentId);
        tenancy.runAs(establishmentId, () -> {
            elbowId = bodySegmentRepository.save(new BodySegment("Codo")).getId();
            shoulderId = bodySegmentRepository.save(new BodySegment("Hombro")).getId();
        });
    }

    @Test
    void adminCreatesAMetricAtTheEndOfTheOrder() throws Exception {
        perform(admin, post(metrics(elbowId)), body("Flexión", "NUMERIC", false, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayOrder").value(1));

        perform(admin, post(metrics(elbowId)), body("Extensión", "NUMERIC", true, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Extensión"))
                .andExpect(jsonPath("$.segmentId").value(elbowId))
                .andExpect(jsonPath("$.answerType").value("NUMERIC"))
                .andExpect(jsonPath("$.required").value(true))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.displayOrder").value(2))
                .andExpect(jsonPath("$.options.length()").value(0));
    }

    @Test
    void optionMetricNeedsTwoDistinctOptions() throws Exception {
        expectError(perform(admin, post(metrics(elbowId)), body("Fin de recorrido", "OPTION", false, List.of("Firme"))),
                ErrorCode.SEGMENT_METRIC_OPTIONS_INVALID);
        expectError(perform(admin, post(metrics(elbowId)),
                body("Fin de recorrido", "OPTION", false, List.of("Firme", "firme"))),
                ErrorCode.SEGMENT_METRIC_OPTIONS_INVALID);
        perform(admin, post(metrics(elbowId)), body("Fin de recorrido", "OPTION", false, List.of("Firme", "Blanda")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.options[0]").value("Firme"))
                .andExpect(jsonPath("$.options[1]").value("Blanda"));
    }

    @Test
    void createWithoutTypeIsInvalid() throws Exception {
        perform(admin, post(metrics(elbowId)), body("Flexión", null, false, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("answerType"));
    }

    @Test
    void nameIsUniquePerSegmentIgnoringCase() throws Exception {
        perform(admin, post(metrics(elbowId)), body("Flexión", "NUMERIC", false, null)).andExpect(status().isCreated());
        expectError(perform(admin, post(metrics(elbowId)), body("flexión", "NUMERIC", false, null)),
                ErrorCode.SEGMENT_METRIC_NAME_TAKEN);
        perform(admin, post(metrics(shoulderId)), body("Flexión", "NUMERIC", false, null))
                .andExpect(status().isCreated());
    }

    @Test
    void editingKeepsTheType() throws Exception {
        Long id = createMetric(elbowId, "Flexión", "NUMERIC", false);

        perform(admin, put(metrics(elbowId) + "/" + id), body("Flexión activa", null, true, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Flexión activa"))
                .andExpect(jsonPath("$.required").value(true))
                .andExpect(jsonPath("$.answerType").value("NUMERIC"));
    }

    @Test
    void deactivateAndReactivate() throws Exception {
        Long id = createMetric(elbowId, "Flexión", "NUMERIC", false);
        String url = metrics(elbowId) + "/" + id + "/active";

        perform(admin, patch(url), Map.of("active", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        perform(admin, patch(url), Map.of("active", true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void reorderFollowsTheGivenIds() throws Exception {
        Long a = createMetric(elbowId, "A", "NUMERIC", false);
        Long b = createMetric(elbowId, "B", "NUMERIC", false);
        Long c = createMetric(elbowId, "C", "NUMERIC", false);
        Long otherSegment = createMetric(shoulderId, "Otra", "NUMERIC", false);

        perform(admin, put(metrics(elbowId) + "/order"), Map.of("ids", List.of(c, a, b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(c))
                .andExpect(jsonPath("$[0].displayOrder").value(1))
                .andExpect(jsonPath("$[1].id").value(a))
                .andExpect(jsonPath("$[1].displayOrder").value(2))
                .andExpect(jsonPath("$[2].id").value(b))
                .andExpect(jsonPath("$[2].displayOrder").value(3));
        JsonNode listed = read(perform(admin, get(metrics(elbowId)), null));
        assertThat(listed.get(0).get("id").asLong()).isEqualTo(c);

        expectError(perform(admin, put(metrics(elbowId) + "/order"), Map.of("ids", List.of(c, a))),
                ErrorCode.SEGMENT_METRIC_ORDER_INVALID);
        expectError(perform(admin, put(metrics(elbowId) + "/order"), Map.of("ids", List.of(c, a, otherSegment))),
                ErrorCode.SEGMENT_METRIC_ORDER_INVALID);
    }

    @Test
    void therapistReadsButCannotWrite() throws Exception {
        perform(therapist, get(metrics(elbowId)), null).andExpect(status().isOk());
        expectError(perform(therapist, post(metrics(elbowId)), body("Flexión", "NUMERIC", false, null)),
                ErrorCode.ACCESS_DENIED);
    }

    @Test
    void countsOnlyActiveMetricsPerSegment() throws Exception {
        createMetric(elbowId, "A", "NUMERIC", false);
        createMetric(elbowId, "B", "NUMERIC", false);
        Long inactive = createMetric(elbowId, "C", "NUMERIC", false);
        perform(admin, patch(metrics(elbowId) + "/" + inactive + "/active"), Map.of("active", false))
                .andExpect(status().isOk());

        perform(therapist, get("/api/segment-metrics/counts"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].segmentId").value(elbowId))
                .andExpect(jsonPath("$[0].count").value(2));
    }

    @Test
    void unknownMetricIs404() throws Exception {
        expectError(perform(admin, put(metrics(elbowId) + "/999999"), body("Flexión", null, false, null)),
                ErrorCode.SEGMENT_METRIC_NOT_FOUND);
    }

    @Test
    void aMetricOfAnotherSegmentIs404() throws Exception {
        Long id = createMetric(elbowId, "Flexión", "NUMERIC", false);

        expectError(perform(admin, put(metrics(shoulderId) + "/" + id), body("Flexión", null, false, null)),
                ErrorCode.SEGMENT_METRIC_NOT_FOUND);
    }

    private Long createMetric(Long segmentId, String name, String type, boolean required) throws Exception {
        return objectMapper.readTree(perform(admin, post(metrics(segmentId)), body(name, type, required, null))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("id").asLong();
    }

    private static String metrics(Long segmentId) {
        return "/api/segments/" + segmentId + "/metrics";
    }

    private static Map<String, Object> body(String name, String answerType, boolean required, List<String> options) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("answerType", answerType);
        body.put("required", required);
        body.put("options", options);
        return body;
    }

    private ResultActions perform(AuthenticatedUser user, MockHttpServletRequestBuilder request, Object content)
            throws Exception {
        request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tenancy.tokenFor(user));
        if (content != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(content));
        }
        return mockMvc.perform(request);
    }

    private JsonNode read(ResultActions result) throws Exception {
        return objectMapper.readTree(
                result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private static void expectError(ResultActions result, ErrorCode code) throws Exception {
        result.andExpect(status().is(code.getHttpStatus().value()))
                .andExpect(jsonPath("$.code").value(code.name()));
    }
}
