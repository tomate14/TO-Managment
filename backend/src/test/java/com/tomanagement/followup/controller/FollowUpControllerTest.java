package com.tomanagement.followup.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomanagement.TestTable;
import com.tomanagement.TestTables;
import com.tomanagement.TestcontainersConfiguration;
import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.auth.security.TokenProvider;
import com.tomanagement.catalog.entity.BodySegment;
import com.tomanagement.catalog.entity.ScarType;
import com.tomanagement.catalog.entity.SurgeryType;
import com.tomanagement.catalog.repository.BodySegmentRepository;
import com.tomanagement.catalog.repository.ScarTypeRepository;
import com.tomanagement.catalog.repository.SurgeryTypeRepository;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.diagnosis.entity.ConservativeTreatment;
import com.tomanagement.diagnosis.entity.Diagnosis;
import com.tomanagement.diagnosis.entity.SurgicalTreatment;
import com.tomanagement.diagnosis.repository.DiagnosisRepository;
import com.tomanagement.patient.entity.Dominance;
import com.tomanagement.patient.entity.Patient;
import com.tomanagement.patient.entity.PatientData;
import com.tomanagement.patient.repository.PatientRepository;
import com.tomanagement.template.entity.ActivationConditionData;
import com.tomanagement.template.entity.AnswerType;
import com.tomanagement.template.entity.ConditionOperator;
import com.tomanagement.template.entity.EvaluationTemplate;
import com.tomanagement.template.entity.Question;
import com.tomanagement.template.entity.QuestionDefinition;
import com.tomanagement.template.repository.EvaluationTemplateRepository;
import com.tomanagement.template.repository.QuestionRepository;
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
 * Follow-ups (H6.1–H6.4) against a real Postgres.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestTables.class})
class FollowUpControllerTest {

    private static final AuthenticatedUser THERAPIST = new AuthenticatedUser(2L, "t@c.com", "Terapeuta", Role.TERAPEUTA, null);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestTables testTables;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private SurgeryTypeRepository surgeryTypeRepository;

    @Autowired
    private ScarTypeRepository scarTypeRepository;

    @Autowired
    private BodySegmentRepository bodySegmentRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private EvaluationTemplateRepository templateRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private Diagnosis surgical;
    private Diagnosis conservative;
    private ScarType normotrophic;
    private ScarType retiredScar;
    private BodySegment wrist;
    private BodySegment hand;
    private EvaluationTemplate wristTemplate;
    private Question splint;
    private Question hours;
    private Question grip;

    @BeforeEach
    void seed() {
        testTables.truncate(TestTable.FOLLOW_UP, TestTable.DIAGNOSIS, TestTable.TREATMENT, TestTable.PATIENT,
                TestTable.EVALUATION_TEMPLATE, TestTable.BODY_SEGMENT, TestTable.SCAR_TYPE, TestTable.SURGERY_TYPE);
        Patient lucia = patientRepository.save(Patient.builder()
                .dni("30123456")
                .data(new PatientData("Lucía", "Fernández", LocalDate.of(1984, 3, 21), "249 412-3456", "Docente", "OSDE",
                        "61 234567 8 01", Dominance.RIGHT))
                .createdAt(Instant.parse("2026-11-04T12:00:00Z"))
                .build());
        SurgeryType plate = surgeryTypeRepository.save(new SurgeryType("Osteosíntesis con placa"));
        surgical = diagnosisRepository.save(new Diagnosis(lucia, "Fractura distal de radio", LocalDate.of(2026, 10, 12),
                new SurgicalTreatment(LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 28), plate)));
        conservative = diagnosisRepository.save(new Diagnosis(lucia, "Esguince", LocalDate.of(2026, 10, 12),
                new ConservativeTreatment(LocalDate.of(2026, 10, 1))));
        normotrophic = scarTypeRepository.save(new ScarType("Normotrófica"));
        retiredScar = new ScarType("Queloide");
        retiredScar.changeActive(false);
        retiredScar = scarTypeRepository.save(retiredScar);
        wrist = bodySegmentRepository.save(new BodySegment("Muñeca"));
        hand = bodySegmentRepository.save(new BodySegment("Mano"));

        wristTemplate = templateRepository.save(new EvaluationTemplate("Fractura de muñeca", wrist));
        splint = saveQuestion(1, "¿Usa férula?", AnswerType.YES_NO, null, null, List.of(), null, null);
        hours = saveQuestion(2, "Horas de uso por día", AnswerType.NUMERIC, BigDecimal.ZERO, new BigDecimal("24"),
                List.of(), splint, new ActivationConditionData(ConditionOperator.EQUALS, "true"));
        grip = saveQuestion(3, "Tipo de agarre afectado", AnswerType.OPTION, null, null,
                List.of("Cilíndrico", "Esférico", "Pinza fina"), null, null);
    }

    @Test
    void registersAWholeFollowUpAndListsItWithItsAnswersInTreeOrder() throws Exception {
        Map<String, Object> wristEvaluation = evaluation(wrist.getId(), "1.5", 3, "55", "14");
        wristEvaluation.put("templateId", wristTemplate.getId());
        wristEvaluation.put("answers", List.of(
                Map.of("questionId", grip.getId(), "optionValue", "cilíndrico"),
                Map.of("questionId", hours.getId(), "numericValue", 8),
                Map.of("questionId", splint.getId(), "booleanValue", true)));
        Map<String, Object> handEvaluation = evaluation(hand.getId(), null, 2, "70", "16");

        perform(post(followUps(surgical)).contentType(MediaType.APPLICATION_JSON)
                .content(json(followUp("2026-10-28", normotrophic.getId(), wristEvaluation, handEvaluation))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.date").value("2026-10-28"))
                .andExpect(jsonPath("$.scarType.name").value("Normotrófica"))
                .andExpect(jsonPath("$.evaluations.length()").value(2))
                .andExpect(jsonPath("$.evaluations[0].segment.name").value("Muñeca"))
                .andExpect(jsonPath("$.evaluations[0].template.name").value("Fractura de muñeca"))
                .andExpect(jsonPath("$.evaluations[0].answers[0].questionText").value("¿Usa férula?"))
                .andExpect(jsonPath("$.evaluations[0].answers[1].questionText").value("Horas de uso por día"))
                .andExpect(jsonPath("$.evaluations[0].answers[1].depth").value(1))
                .andExpect(jsonPath("$.evaluations[0].answers[2].optionValue").value("Cilíndrico"))
                .andExpect(jsonPath("$.evaluations[1].edema").doesNotExist())
                .andExpect(jsonPath("$.evaluations[1].template").doesNotExist());

        perform(get(followUps(surgical)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].evaluations[0].answers.length()").value(3));
        perform(get("/api/diagnoses/" + surgical.getId()))
                .andExpect(jsonPath("$.followUpCount").value(1));
    }

    @Test
    void listsNewestFirst() throws Exception {
        register(followUp("2026-10-21", normotrophic.getId(), evaluation(wrist.getId(), null, 5, "40", "9")));
        register(followUp("2026-10-28", normotrophic.getId(), evaluation(wrist.getId(), null, 3, "55", "14")));

        perform(get(followUps(surgical)))
                .andExpect(jsonPath("$[0].date").value("2026-10-28"))
                .andExpect(jsonPath("$[1].date").value("2026-10-21"));
    }

    @Test
    void surgicalNeedsAnActiveScarTypeAndConservativeRejectsIt() throws Exception {
        expectError(surgical, followUp("2026-10-28", null, evaluation(wrist.getId(), null, 3, "55", "14")),
                ErrorCode.SCAR_TYPE_REQUIRED);
        expectError(surgical, followUp("2026-10-28", retiredScar.getId(), evaluation(wrist.getId(), null, 3, "55", "14")),
                ErrorCode.SCAR_TYPE_INACTIVE);
        expectError(conservative, followUp("2026-10-28", normotrophic.getId(), evaluation(wrist.getId(), null, 3, "55", "14")),
                ErrorCode.SCAR_TYPE_NOT_ALLOWED);
        perform(post(followUps(conservative)).contentType(MediaType.APPLICATION_JSON)
                .content(json(followUp("2026-10-28", null, evaluation(wrist.getId(), null, 3, "55", "14")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scarType").doesNotExist());
    }

    @Test
    void outOfRangeMetricsComeBackOnePerField() throws Exception {
        perform(post(followUps(surgical)).contentType(MediaType.APPLICATION_JSON)
                .content(json(followUp("2026-10-28", normotrophic.getId(), evaluation(wrist.getId(), "1.5", 11, "550", "14")))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCode.FOLLOW_UP_OUT_OF_RANGE.name()))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'evaluations[0].rom')].message")
                        .value("ROM fuera del rango permitido."))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'evaluations[0].pain')]").exists());
    }

    @Test
    void needsAtLeastOneEvaluationAndTheRequiredMetrics() throws Exception {
        perform(post(followUps(surgical)).contentType(MediaType.APPLICATION_JSON)
                .content(json(followUp("2026-10-28", normotrophic.getId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()));
        perform(post(followUps(surgical)).contentType(MediaType.APPLICATION_JSON)
                .content(json(followUp("2026-10-28", normotrophic.getId(), evaluation(wrist.getId(), null, 3, null, null)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }

    @Test
    void segmentsCannotRepeat() throws Exception {
        expectError(surgical, followUp("2026-10-28", normotrophic.getId(), evaluation(wrist.getId(), null, 3, "55", "14"),
                evaluation(wrist.getId(), null, 2, "60", "15")), ErrorCode.SEGMENT_REPEATED);
    }

    @Test
    void theTemplateMustBelongToTheSegment() throws Exception {
        Map<String, Object> handEvaluation = evaluation(hand.getId(), null, 2, "70", "16");
        handEvaluation.put("templateId", wristTemplate.getId());
        expectError(surgical, followUp("2026-10-28", normotrophic.getId(), handEvaluation), ErrorCode.TEMPLATE_NOT_AVAILABLE);
    }

    @Test
    void answersToHiddenQuestionsAreRejected() throws Exception {
        Map<String, Object> wristEvaluation = evaluation(wrist.getId(), null, 3, "55", "14");
        wristEvaluation.put("templateId", wristTemplate.getId());
        wristEvaluation.put("answers", List.of(
                Map.of("questionId", splint.getId(), "booleanValue", false),
                Map.of("questionId", hours.getId(), "numericValue", 8)));
        expectError(surgical, followUp("2026-10-28", normotrophic.getId(), wristEvaluation), ErrorCode.ANSWER_QUESTION_INVALID);
    }

    @Test
    void invalidAnswerValuesAreRejected() throws Exception {
        Map<String, Object> wristEvaluation = evaluation(wrist.getId(), null, 3, "55", "14");
        wristEvaluation.put("templateId", wristTemplate.getId());
        wristEvaluation.put("answers", List.of(
                Map.of("questionId", splint.getId(), "booleanValue", true),
                Map.of("questionId", hours.getId(), "numericValue", 30)));
        expectError(surgical, followUp("2026-10-28", normotrophic.getId(), wristEvaluation), ErrorCode.ANSWER_VALUE_INVALID);
    }

    @Test
    void answeredQuestionsBecomeLocked() throws Exception {
        Map<String, Object> wristEvaluation = evaluation(wrist.getId(), null, 3, "55", "14");
        wristEvaluation.put("templateId", wristTemplate.getId());
        wristEvaluation.put("answers", List.of(Map.of("questionId", splint.getId(), "booleanValue", false)));
        register(followUp("2026-10-28", normotrophic.getId(), wristEvaluation));

        perform(get("/api/templates/" + wristTemplate.getId()))
                .andExpect(jsonPath("$.questions[0].locked").value(true))
                .andExpect(jsonPath("$.questions[1].locked").value(false));
    }

    @Test
    void unknownDiagnosisIsNotFound() throws Exception {
        perform(get("/api/diagnoses/999999/follow-ups"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.DIAGNOSIS_NOT_FOUND.name()));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get(followUps(surgical))).andExpect(status().isUnauthorized());
    }

    private Question saveQuestion(int order, String text, AnswerType type, BigDecimal min, BigDecimal max,
            List<String> options, Question parent, ActivationConditionData condition) {
        Question question = new Question(wristTemplate, order);
        question.define(new QuestionDefinition(text, type, min, max, options, parent, condition));
        return questionRepository.save(question);
    }

    private void register(Map<String, Object> body) throws Exception {
        perform(post(followUps(surgical)).contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated());
    }

    private void expectError(Diagnosis diagnosis, Map<String, Object> body, ErrorCode code) throws Exception {
        perform(post(followUps(diagnosis)).contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(code.name()));
    }

    private static String followUps(Diagnosis diagnosis) {
        return "/api/diagnoses/" + diagnosis.getId() + "/follow-ups";
    }

    @SafeVarargs
    private static Map<String, Object> followUp(String date, Long scarTypeId, Map<String, Object>... evaluations) {
        Map<String, Object> body = new HashMap<>();
        body.put("date", date);
        body.put("scarTypeId", scarTypeId);
        body.put("evaluations", new ArrayList<>(List.of(evaluations)));
        return body;
    }

    private static Map<String, Object> evaluation(Long segmentId, String edema, Integer pain, String rom, String strength) {
        Map<String, Object> evaluation = new HashMap<>();
        evaluation.put("segmentId", segmentId);
        evaluation.put("edema", edema == null ? null : new BigDecimal(edema));
        evaluation.put("pain", pain);
        evaluation.put("rom", rom == null ? null : new BigDecimal(rom));
        evaluation.put("strength", strength == null ? null : new BigDecimal(strength));
        return evaluation;
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.issue(THERAPIST).value()));
    }

    private String json(Map<String, ?> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }
}
