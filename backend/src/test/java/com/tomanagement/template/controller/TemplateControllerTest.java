package com.tomanagement.template.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomanagement.TestTable;
import com.tomanagement.TestTables;
import com.tomanagement.TestcontainersConfiguration;
import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.auth.security.TokenProvider;
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

/**
 * Templates and questions (H3.1–H3.4) against a real Postgres, as a therapist.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestTables.class})
class TemplateControllerTest {

    private static final String TEMPLATES = "/api/templates";
    private static final AuthenticatedUser THERAPIST = new AuthenticatedUser(2L, "t@c.com", "Terapeuta", Role.TERAPEUTA, null);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestTables testTables;

    @Autowired
    private BodySegmentRepository segmentRepository;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private BodySegment wrist;
    private BodySegment inactiveSegment;

    @BeforeEach
    void cleanUp() {
        testTables.truncate(TestTable.EVALUATION_TEMPLATE, TestTable.BODY_SEGMENT);
        wrist = segmentRepository.save(new BodySegment("Muñeca"));
        inactiveSegment = new BodySegment("Dedos");
        inactiveSegment.changeActive(false);
        inactiveSegment = segmentRepository.save(inactiveSegment);
    }

    @Test
    void createsATemplateAndListsItWithCounts() throws Exception {
        long templateId = createTemplate("Fractura de muñeca");
        long splint = addQuestion(templateId, Map.of("text", "¿Usa férula?", "answerType", "YES_NO"));
        addQuestion(templateId, chained("Horas de uso por día", "NUMERIC", splint, "EQUALS", "true"));

        perform(get(TEMPLATES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Fractura de muñeca"))
                .andExpect(jsonPath("$[0].segment.name").value("Muñeca"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].questionCount").value(2))
                .andExpect(jsonPath("$[0].chainedCount").value(1));
        perform(get(TEMPLATES).param("segmentId", String.valueOf(inactiveSegment.getId())))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void templateNameIsUniqueWithinTheSegment() throws Exception {
        createTemplate("Fractura de muñeca");

        perform(post(TEMPLATES).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "FRACTURA de muñeca", "segmentId", wrist.getId()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.TEMPLATE_DUPLICATE.name()));
    }

    @Test
    void templatesNeedAnActiveSegment() throws Exception {
        perform(post(TEMPLATES).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Dedos en martillo", "segmentId", inactiveSegment.getId()))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCode.SEGMENT_INACTIVE.name()));
    }

    @Test
    void editsAndDeactivatesATemplate() throws Exception {
        long templateId = createTemplate("Fractura de muñeca");

        perform(put(TEMPLATES + "/" + templateId).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Fractura distal de radio", "segmentId", wrist.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Fractura distal de radio"));
        perform(patch(TEMPLATES + "/" + templateId + "/active").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void detailListsChildrenRightAfterTheirParentWithDepthAndNumbers() throws Exception {
        long templateId = createTemplate("Fractura de muñeca");
        long splint = addQuestion(templateId, Map.of("text", "¿Usa férula?", "answerType", "YES_NO"));
        addQuestion(templateId, Map.of("text", "Observaciones", "answerType", "TEXT"));
        addQuestion(templateId, chained("Horas de uso por día", "NUMERIC", splint, "EQUALS", "true"));

        perform(get(TEMPLATES + "/" + templateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].text").value("¿Usa férula?"))
                .andExpect(jsonPath("$.questions[0].hasDependents").value(true))
                .andExpect(jsonPath("$.questions[1].text").value("Horas de uso por día"))
                .andExpect(jsonPath("$.questions[1].number").value(2))
                .andExpect(jsonPath("$.questions[1].depth").value(1))
                .andExpect(jsonPath("$.questions[1].condition.operator").value("EQUALS"))
                .andExpect(jsonPath("$.questions[1].condition.expectedValue").value("true"))
                .andExpect(jsonPath("$.questions[2].text").value("Observaciones"))
                .andExpect(jsonPath("$.questions[2].locked").value(false));
    }

    @Test
    void numericRangeMustBeOrdered() throws Exception {
        long templateId = createTemplate("Fractura de muñeca");

        perform(post(questions(templateId)).contentType(MediaType.APPLICATION_JSON).content(json(Map.of(
                        "text", "Horas", "answerType", "NUMERIC", "minValue", 24, "maxValue", 0))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCode.QUESTION_RANGE_INVALID.name()));
    }

    @Test
    void optionQuestionsNeedTwoOptions() throws Exception {
        long templateId = createTemplate("Fractura de muñeca");

        perform(post(questions(templateId)).contentType(MediaType.APPLICATION_JSON).content(json(Map.of(
                        "text", "Tipo de agarre", "answerType", "OPTION", "options", List.of("Cilíndrico")))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCode.QUESTION_OPTIONS_REQUIRED.name()));
    }

    @Test
    void greaterThanOnAYesNoParentIsRejectedWithAClearError() throws Exception {
        long templateId = createTemplate("Fractura de muñeca");
        long splint = addQuestion(templateId, Map.of("text", "¿Usa férula?", "answerType", "YES_NO"));

        perform(post(questions(templateId)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(chained("Horas", "NUMERIC", splint, "GREATER_THAN", "true"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCode.CONDITION_OPERATOR_INCOMPATIBLE.name()));
    }

    @Test
    void expectedValueMustBeAnOptionOfTheParent() throws Exception {
        long templateId = createTemplate("Fractura de muñeca");
        long grip = addQuestion(templateId, Map.of("text", "Tipo de agarre", "answerType", "OPTION",
                "options", List.of("Cilíndrico", "Esférico")));

        perform(post(questions(templateId)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(chained("Detalle", "TEXT", grip, "EQUALS", "Palmar"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCode.CONDITION_VALUE_INVALID.name()));
        perform(post(questions(templateId)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(chained("Detalle", "TEXT", grip, "EQUALS", "esférico"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.questions[1].condition.expectedValue").value("Esférico"));
    }

    @Test
    void parentMustBeAnEarlierQuestionOfTheSameTemplate() throws Exception {
        long templateId = createTemplate("Fractura de muñeca");
        long other = createTemplate("Túnel carpiano");
        long foreign = addQuestion(other, Map.of("text", "¿Hormigueo?", "answerType", "YES_NO"));

        perform(post(questions(templateId)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(chained("Horas", "NUMERIC", foreign, "EQUALS", "true"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCode.CONDITION_PARENT_INVALID.name()));
    }

    @Test
    void aParentCannotChangeTypeOrBeDeletedWhileOthersDependOnIt() throws Exception {
        long templateId = createTemplate("Fractura de muñeca");
        long splint = addQuestion(templateId, Map.of("text", "¿Usa férula?", "answerType", "YES_NO"));
        addQuestion(templateId, chained("Horas", "NUMERIC", splint, "EQUALS", "true"));

        perform(put(questions(templateId) + "/" + splint).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("text", "¿Usa férula?", "answerType", "TEXT"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCode.QUESTION_HAS_DEPENDENTS.name()));
        perform(put(questions(templateId) + "/" + splint).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("text", "¿Usa férula de reposo?", "answerType", "YES_NO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].text").value("¿Usa férula de reposo?"));
        perform(delete(questions(templateId) + "/" + splint))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCode.QUESTION_HAS_DEPENDENTS.name()));
    }

    @Test
    void deletesAndDeactivatesQuestions() throws Exception {
        long templateId = createTemplate("Fractura de muñeca");
        long splint = addQuestion(templateId, Map.of("text", "¿Usa férula?", "answerType", "YES_NO"));
        long notes = addQuestion(templateId, Map.of("text", "Observaciones", "answerType", "TEXT"));

        perform(patch(questions(templateId) + "/" + splint + "/active").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].active").value(false));
        perform(delete(questions(templateId) + "/" + notes)).andExpect(status().isNoContent());
        perform(get(TEMPLATES + "/" + templateId)).andExpect(jsonPath("$.questions.length()").value(1));
    }

    @Test
    void unknownTemplateIsNotFound() throws Exception {
        perform(get(TEMPLATES + "/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.TEMPLATE_NOT_FOUND.name()));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get(TEMPLATES)).andExpect(status().isUnauthorized());
    }

    private long createTemplate(String name) throws Exception {
        String body = perform(post(TEMPLATES).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", name, "segmentId", wrist.getId()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    /**
     * @return the id of the question just added (the one with the highest displayOrder)
     */
    private long addQuestion(long templateId, Map<String, Object> request) throws Exception {
        String body = perform(post(questions(templateId)).contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode newest = null;
        for (JsonNode question : objectMapper.readTree(body).get("questions")) {
            if (newest == null || question.get("displayOrder").asInt() > newest.get("displayOrder").asInt()) {
                newest = question;
            }
        }
        return newest.get("id").asLong();
    }

    private static Map<String, Object> chained(String text, String type, long parentId, String operator, String value) {
        Map<String, Object> request = new HashMap<>();
        request.put("text", text);
        request.put("answerType", type);
        request.put("parentQuestionId", parentId);
        request.put("condition", Map.of("operator", operator, "expectedValue", value));
        return request;
    }

    private static String questions(long templateId) {
        return TEMPLATES + "/" + templateId + "/questions";
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION,
                "Bearer " + tokenProvider.issue(THERAPIST).value()));
    }

    private String json(Map<String, ?> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }
}
