package com.tomanagement.followup.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomanagement.TestTable;
import com.tomanagement.TestTables;
import com.tomanagement.TestTenancy;
import com.tomanagement.TestcontainersConfiguration;
import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.billing.entity.Billing;
import com.tomanagement.billing.repository.BillingRepository;
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
import com.tomanagement.followup.entity.ClinicalMetrics;
import com.tomanagement.followup.entity.FollowUp;
import com.tomanagement.followup.repository.FollowUpRepository;
import com.tomanagement.patient.entity.Dominance;
import com.tomanagement.patient.entity.Patient;
import com.tomanagement.patient.entity.PatientData;
import com.tomanagement.patient.repository.PatientRepository;
import com.tomanagement.segmentmetric.entity.SegmentMetric;
import com.tomanagement.segmentmetric.repository.SegmentMetricRepository;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Follow-ups (H6.1–H6.4) against a real Postgres.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestTables.class, TestTenancy.class})
class FollowUpControllerTest {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BillingRepository billingRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

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
    private SegmentMetricRepository metricRepository;

    @Autowired
    private FollowUpRepository followUpRepository;

    @Autowired
    private TestTenancy tenancy;

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
    private SegmentMetric flexion;
    private SegmentMetric complete;
    private SegmentMetric feel;
    private SegmentMetric notes;
    private SegmentMetric oldMetric;
    private SegmentMetric handOpening;
    private Long establishmentId;
    private AuthenticatedUser therapist;

    @BeforeEach
    void seed() {
        testTables.truncate(TestTable.FOLLOW_UP, TestTable.DIAGNOSIS, TestTable.TREATMENT, TestTable.PATIENT,
                TestTable.EVALUATION_TEMPLATE, TestTable.BODY_SEGMENT, TestTable.SCAR_TYPE, TestTable.SURGERY_TYPE,
                TestTable.ESTABLISHMENT);
        establishmentId = tenancy.create("Consultorio").getId();
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
        SurgeryType plate = surgeryTypeRepository.save(new SurgeryType("Osteosíntesis con placa"));
        surgical = diagnosisRepository.save(new Diagnosis(lucia, "Fractura distal de radio", LocalDate.of(2026, 10, 12),
                new SurgicalTreatment(LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 28), plate)));
        conservative = diagnosisRepository.save(new Diagnosis(lucia, "Esguince", LocalDate.of(2026, 10, 12),
                new ConservativeTreatment(true, LocalDate.of(2026, 10, 1))));
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

        flexion = metricRepository.save(new SegmentMetric(wrist, "Flexión", AnswerType.NUMERIC, true, 1, List.of()));
        complete = metricRepository.save(new SegmentMetric(wrist, "Rango completo", AnswerType.YES_NO, false, 2,
                List.of()));
        feel = metricRepository.save(new SegmentMetric(wrist, "Tope final", AnswerType.OPTION, false, 3,
                List.of("Firme", "Blanda")));
        notes = metricRepository.save(new SegmentMetric(wrist, "Observaciones", AnswerType.TEXT, false, 4, List.of()));
        SegmentMetric retired = new SegmentMetric(wrist, "Extensión vieja", AnswerType.NUMERIC, false, 5, List.of());
        retired.changeActive(false);
        oldMetric = metricRepository.save(retired);
        handOpening = metricRepository.save(new SegmentMetric(hand, "Apertura", AnswerType.NUMERIC, false, 1,
                List.of()));
    }

    @Test
    void registersAWholeFollowUpAndListsItWithItsAnswersInTreeOrder() throws Exception {
        Map<String, Object> wristEvaluation = evaluation(wrist.getId(), "1.5", 3, "14", flexionOf("55"));
        wristEvaluation.put("templateId", wristTemplate.getId());
        wristEvaluation.put("answers", List.of(
                Map.of("questionId", grip.getId(), "optionValue", "cilíndrico"),
                Map.of("questionId", hours.getId(), "numericValue", 8),
                Map.of("questionId", splint.getId(), "booleanValue", true)));
        Map<String, Object> handEvaluation = evaluation(hand.getId(), null, 2, "16", List.of());

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
    void followUpsCarryTheBilledMark() throws Exception {
        register(followUp("2026-10-21", normotrophic.getId(), evaluation(wrist.getId(), null, 5, "9", flexionOf("40"))));
        register(followUp("2026-10-28", normotrophic.getId(), evaluation(wrist.getId(), null, 3, "14", flexionOf("55"))));
        String listing = perform(get(followUps(surgical))).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        Long followUpId = objectMapper.readTree(listing).get(1).get("id").asLong();
        Long billingId = tenancy.callAs(therapist.establishmentId(), () -> new TransactionTemplate(transactionManager)
                .execute(transaction -> {
                    FollowUp historical = followUpRepository.findById(followUpId).orElseThrow();
                    return billingRepository.saveAndFlush(new Billing(historical.getDiagnosis(),
                            LocalDate.of(2026, 10, 30), new BigDecimal("25000"),
                            Instant.parse("2026-10-30T12:00:00Z"), List.of(historical))).getId();
                }));

        perform(get(followUps(surgical)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2026-10-28"))
                .andExpect(jsonPath("$[0].billed").value(false))
                .andExpect(jsonPath("$[1].date").value("2026-10-21"))
                .andExpect(jsonPath("$[1].billed").value(true));
        perform(post("/api/billings/" + billingId + "/void")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSessions").value(0))
                .andExpect(jsonPath("$.pendingSessions").value(0))
                .andExpect(jsonPath("$.billings[0].sessions[0].segments[0]").value("Muñeca"));
        perform(get(followUps(surgical))).andExpect(status().isOk())
                .andExpect(jsonPath("$[1].billed").value(false));
    }

    @Test
    void listsNewestFirst() throws Exception {
        register(followUp("2026-10-21", normotrophic.getId(), evaluation(wrist.getId(), null, 5, "9", flexionOf("40"))));
        register(followUp("2026-10-28", normotrophic.getId(), evaluation(wrist.getId(), null, 3, "14", flexionOf("55"))));

        perform(get(followUps(surgical)))
                .andExpect(jsonPath("$[0].date").value("2026-10-28"))
                .andExpect(jsonPath("$[1].date").value("2026-10-21"));
    }

    @Test
    void surgicalNeedsAnActiveScarTypeAndConservativeRejectsIt() throws Exception {
        expectError(surgical, followUp("2026-10-28", null, evaluation(wrist.getId(), null, 3, "14", flexionOf("55"))),
                ErrorCode.SCAR_TYPE_REQUIRED);
        expectError(surgical, followUp("2026-10-28", retiredScar.getId(), evaluation(wrist.getId(), null, 3, "14", flexionOf("55"))),
                ErrorCode.SCAR_TYPE_INACTIVE);
        expectError(conservative, followUp("2026-10-28", normotrophic.getId(), evaluation(wrist.getId(), null, 3, "14", flexionOf("55"))),
                ErrorCode.SCAR_TYPE_NOT_ALLOWED);
        perform(post(followUps(conservative)).contentType(MediaType.APPLICATION_JSON)
                .content(json(followUp("2026-10-28", null, evaluation(wrist.getId(), null, 3, "14", flexionOf("55"))))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scarType").doesNotExist());
    }

    @Test
    void outOfRangeMetricsComeBackOnePerField() throws Exception {
        perform(post(followUps(surgical)).contentType(MediaType.APPLICATION_JSON)
                .content(json(followUp("2026-10-28", normotrophic.getId(), evaluation(wrist.getId(), "1.5", 11, "140", flexionOf("550"))))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCode.FOLLOW_UP_OUT_OF_RANGE.name()))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'evaluations[0].strength')].message")
                        .value("Fuerza fuera del rango permitido."))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'evaluations[0].pain')]").exists());
    }

    @Test
    void needsAtLeastOneEvaluationAndTheRequiredMetrics() throws Exception {
        perform(post(followUps(surgical)).contentType(MediaType.APPLICATION_JSON)
                .content(json(followUp("2026-10-28", normotrophic.getId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()));
        perform(post(followUps(surgical)).contentType(MediaType.APPLICATION_JSON)
                .content(json(followUp("2026-10-28", normotrophic.getId(), evaluation(wrist.getId(), null, 3, null, List.of())))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(1));
    }

    @Test
    void savesMetricValuesWithoutRom() throws Exception {
        Map<String, Object> wristEvaluation = evaluation(wrist.getId(), null, 3, "14", List.of(
                metric(feel.getId(), "firme"),
                metric(complete.getId(), true),
                metric(flexion.getId(), "130")));

        perform(post(followUps(surgical)).contentType(MediaType.APPLICATION_JSON)
                .content(json(followUp("2026-10-28", normotrophic.getId(), wristEvaluation))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.evaluations[0].rom").doesNotExist())
                .andExpect(jsonPath("$.evaluations[0].metrics.length()").value(3))
                .andExpect(jsonPath("$.evaluations[0].metrics[0].metricId").value(flexion.getId()))
                .andExpect(jsonPath("$.evaluations[0].metrics[0].name").value("Flexión"))
                .andExpect(jsonPath("$.evaluations[0].metrics[0].answerType").value("NUMERIC"))
                .andExpect(jsonPath("$.evaluations[0].metrics[0].numericValue").value(130.0))
                .andExpect(jsonPath("$.evaluations[0].metrics[1].booleanValue").value(true))
                .andExpect(jsonPath("$.evaluations[0].metrics[2].optionValue").value("Firme"));

        perform(get(followUps(surgical)))
                .andExpect(jsonPath("$[0].evaluations[0].rom").doesNotExist())
                .andExpect(jsonPath("$[0].evaluations[0].metrics.length()").value(3))
                .andExpect(jsonPath("$[0].evaluations[0].metrics[0].numericValue").value(130.0))
                .andExpect(jsonPath("$[0].evaluations[0].metrics[2].optionValue").value("Firme"));
    }

    @Test
    void optionalMetricsMayBeOmitted() throws Exception {
        perform(post(followUps(surgical)).contentType(MediaType.APPLICATION_JSON)
                .content(json(followUp("2026-10-28", normotrophic.getId(),
                        evaluation(wrist.getId(), null, 3, "14", flexionOf("130"))))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.evaluations[0].metrics.length()").value(1));
    }

    @Test
    void missingRequiredMetricIsAFieldError() throws Exception {
        perform(post(followUps(surgical)).contentType(MediaType.APPLICATION_JSON)
                .content(json(followUp("2026-10-28", normotrophic.getId(),
                        evaluation(wrist.getId(), null, 3, "14", List.of(metric(complete.getId(), true)))))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCode.METRIC_VALUE_REQUIRED.name()))
                .andExpect(jsonPath("$.fieldErrors.length()").value(1))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'evaluations[0].metrics[" + flexion.getId()
                        + "]')].message").value("Completá esta métrica."));
    }

    @Test
    void wrongTypeOrUnknownOptionIsInvalid() throws Exception {
        perform(post(followUps(surgical)).contentType(MediaType.APPLICATION_JSON)
                .content(json(followUp("2026-10-28", normotrophic.getId(),
                        evaluation(wrist.getId(), null, 3, "14", List.of(metric(flexion.getId(), true)))))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCode.METRIC_VALUE_INVALID.name()))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'evaluations[0].metrics[" + flexion.getId()
                        + "]')].message").value("El valor no es válido para esta métrica."));
        expectError(surgical, followUp("2026-10-28", normotrophic.getId(), evaluation(wrist.getId(), null, 3, "14",
                List.of(metric(flexion.getId(), "130"), metric(feel.getId(), "Vacía")))),
                ErrorCode.METRIC_VALUE_INVALID);
    }

    @Test
    void metricValueBeyondTheStoredPrecisionIsInvalid() throws Exception {
        perform(post(followUps(surgical)).contentType(MediaType.APPLICATION_JSON)
                .content(json(followUp("2026-10-28", normotrophic.getId(),
                        evaluation(wrist.getId(), null, 3, "14", List.of(metric(flexion.getId(), 10000000000L)))))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCode.METRIC_VALUE_INVALID.name()));
    }

    @Test
    void metricOfAnotherSegmentOrInactiveIsRejected() throws Exception {
        expectError(surgical, followUp("2026-10-28", normotrophic.getId(), evaluation(wrist.getId(), null, 3, "14",
                List.of(metric(flexion.getId(), "130"), metric(handOpening.getId(), "40")))),
                ErrorCode.METRIC_NOT_OF_SEGMENT);
        expectError(surgical, followUp("2026-10-28", normotrophic.getId(), evaluation(wrist.getId(), null, 3, "14",
                List.of(metric(flexion.getId(), "130"), metric(oldMetric.getId(), "40")))),
                ErrorCode.METRIC_NOT_OF_SEGMENT);
    }

    @Test
    void repeatedMetricIsRejected() throws Exception {
        expectError(surgical, followUp("2026-10-28", normotrophic.getId(), evaluation(wrist.getId(), null, 3, "14",
                List.of(metric(flexion.getId(), "130"), metric(flexion.getId(), "120")))),
                ErrorCode.METRIC_VALUE_INVALID);
    }

    @Test
    void deactivatedMetricStillShowsInOldFollowUps() throws Exception {
        register(followUp("2026-10-28", normotrophic.getId(), evaluation(wrist.getId(), null, 3, "14",
                List.of(metric(flexion.getId(), "130"), metric(complete.getId(), false),
                        metric(notes.getId(), "Sin dolor al final del rango", true)))));
        tenancy.runAs(establishmentId, () -> {
            SegmentMetric metric = metricRepository.findById(complete.getId()).orElseThrow();
            metric.changeActive(false);
            metricRepository.save(metric);
        });

        perform(get(followUps(surgical)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].evaluations[0].metrics.length()").value(3))
                .andExpect(jsonPath("$[0].evaluations[0].metrics[1].name").value("Rango completo"))
                .andExpect(jsonPath("$[0].evaluations[0].metrics[1].booleanValue").value(false))
                .andExpect(jsonPath("$[0].evaluations[0].metrics[2].textValue").value("Sin dolor al final del rango"));
    }

    @Test
    void legacyFollowUpKeepsItsRom() throws Exception {
        tenancy.runAs(establishmentId, () -> {
            FollowUp legacy = FollowUp.builder()
                    .diagnosis(surgical)
                    .date(LocalDate.of(2026, 10, 21))
                    .scarType(normotrophic)
                    .build();
            legacy.evaluate(wrist, new ClinicalMetrics(null, 4, new BigDecimal("90"), new BigDecimal("12")), null);
            followUpRepository.save(legacy);
        });

        perform(get(followUps(surgical)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].evaluations[0].rom").value(90.0))
                .andExpect(jsonPath("$[0].evaluations[0].metrics.length()").value(0));
    }

    @Test
    void segmentsCannotRepeat() throws Exception {
        expectError(surgical, followUp("2026-10-28", normotrophic.getId(), evaluation(wrist.getId(), null, 3, "14", flexionOf("55")),
                evaluation(wrist.getId(), null, 2, "15", flexionOf("60"))), ErrorCode.SEGMENT_REPEATED);
    }

    @Test
    void theTemplateMustBelongToTheSegment() throws Exception {
        Map<String, Object> handEvaluation = evaluation(hand.getId(), null, 2, "16", List.of());
        handEvaluation.put("templateId", wristTemplate.getId());
        expectError(surgical, followUp("2026-10-28", normotrophic.getId(), handEvaluation), ErrorCode.TEMPLATE_NOT_AVAILABLE);
    }

    @Test
    void answersToHiddenQuestionsAreRejected() throws Exception {
        Map<String, Object> wristEvaluation = evaluation(wrist.getId(), null, 3, "14", flexionOf("55"));
        wristEvaluation.put("templateId", wristTemplate.getId());
        wristEvaluation.put("answers", List.of(
                Map.of("questionId", splint.getId(), "booleanValue", false),
                Map.of("questionId", hours.getId(), "numericValue", 8)));
        expectError(surgical, followUp("2026-10-28", normotrophic.getId(), wristEvaluation), ErrorCode.ANSWER_QUESTION_INVALID);
    }

    @Test
    void invalidAnswerValuesAreRejected() throws Exception {
        Map<String, Object> wristEvaluation = evaluation(wrist.getId(), null, 3, "14", flexionOf("55"));
        wristEvaluation.put("templateId", wristTemplate.getId());
        wristEvaluation.put("answers", List.of(
                Map.of("questionId", splint.getId(), "booleanValue", true),
                Map.of("questionId", hours.getId(), "numericValue", 30)));
        expectError(surgical, followUp("2026-10-28", normotrophic.getId(), wristEvaluation), ErrorCode.ANSWER_VALUE_INVALID);
    }

    @Test
    void answeredQuestionsBecomeLocked() throws Exception {
        Map<String, Object> wristEvaluation = evaluation(wrist.getId(), null, 3, "14", flexionOf("55"));
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

    private static Map<String, Object> evaluation(Long segmentId, String edema, Integer pain, String strength,
            List<Map<String, Object>> metrics) {
        Map<String, Object> evaluation = new HashMap<>();
        evaluation.put("segmentId", segmentId);
        evaluation.put("edema", edema == null ? null : new BigDecimal(edema));
        evaluation.put("pain", pain);
        evaluation.put("strength", strength == null ? null : new BigDecimal(strength));
        evaluation.put("metrics", new ArrayList<>(metrics));
        return evaluation;
    }

    private List<Map<String, Object>> flexionOf(String value) {
        return List.of(metric(flexion.getId(), value));
    }

    /**
     * A metric value: BigDecimal or numeric String → numericValue, Boolean → booleanValue, other String →
     * optionValue.
     */
    private static Map<String, Object> metric(Long metricId, Object value) {
        return metric(metricId, value, false);
    }

    /**
     * @param asText sends a non-numeric String as textValue instead of optionValue
     */
    private static Map<String, Object> metric(Long metricId, Object value, boolean asText) {
        Map<String, Object> metric = new HashMap<>();
        metric.put("metricId", metricId);
        if (value instanceof BigDecimal number) {
            metric.put("numericValue", number);
        } else if (value instanceof Boolean yesNo) {
            metric.put("booleanValue", yesNo);
        } else if (!asText && value instanceof String text && text.matches("-?\\d+(\\.\\d+)?")) {
            metric.put("numericValue", new BigDecimal(text));
        } else if (asText) {
            metric.put("textValue", value);
        } else {
            metric.put("optionValue", value);
        }
        return metric;
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tenancy.tokenFor(therapist)));
    }

    private String json(Map<String, ?> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }
}
