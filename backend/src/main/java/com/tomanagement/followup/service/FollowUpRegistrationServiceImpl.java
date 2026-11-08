package com.tomanagement.followup.service;

import java.util.List;
import java.util.Set;

import com.tomanagement.catalog.entity.BodySegment;
import com.tomanagement.catalog.entity.ScarType;
import com.tomanagement.catalog.repository.BodySegmentRepository;
import com.tomanagement.catalog.repository.ScarTypeRepository;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.ResourceNotFoundException;
import com.tomanagement.diagnosis.entity.Diagnosis;
import com.tomanagement.diagnosis.repository.DiagnosisRepository;
import com.tomanagement.followup.dto.CreateFollowUpRequest;
import com.tomanagement.followup.dto.FollowUpResponse;
import com.tomanagement.followup.dto.SegmentEvaluationRequest;
import com.tomanagement.followup.entity.ClinicalMetrics;
import com.tomanagement.followup.entity.FollowUp;
import com.tomanagement.followup.entity.SegmentEvaluation;
import com.tomanagement.followup.mapper.FollowUpMapper;
import com.tomanagement.followup.repository.FollowUpRepository;
import com.tomanagement.followup.service.TemplateAnswerReader.TemplateAnswers;
import com.tomanagement.followup.validation.FollowUpValidationChain;
import com.tomanagement.followup.validation.FollowUpValidationContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowUpRegistrationServiceImpl implements FollowUpRegistrationService {

    private final DiagnosisRepository diagnosisRepository;
    private final ScarTypeRepository scarTypeRepository;
    private final BodySegmentRepository bodySegmentRepository;
    private final FollowUpRepository followUpRepository;
    private final FollowUpValidationChain validationChain;
    private final TemplateAnswerReader answerReader;
    private final SegmentMetricValueReader metricValueReader;
    private final TemplateQuestionsLoader questionsLoader;
    private final FollowUpMapper mapper;

    public FollowUpRegistrationServiceImpl(DiagnosisRepository diagnosisRepository,
            ScarTypeRepository scarTypeRepository, BodySegmentRepository bodySegmentRepository,
            FollowUpRepository followUpRepository, FollowUpValidationChain validationChain,
            TemplateAnswerReader answerReader, SegmentMetricValueReader metricValueReader,
            TemplateQuestionsLoader questionsLoader, FollowUpMapper mapper) {
        this.diagnosisRepository = diagnosisRepository;
        this.scarTypeRepository = scarTypeRepository;
        this.bodySegmentRepository = bodySegmentRepository;
        this.followUpRepository = followUpRepository;
        this.validationChain = validationChain;
        this.answerReader = answerReader;
        this.metricValueReader = metricValueReader;
        this.questionsLoader = questionsLoader;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public FollowUpResponse register(Long diagnosisId, CreateFollowUpRequest request) {
        Diagnosis diagnosis = diagnosisRepository.findById(diagnosisId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DIAGNOSIS_NOT_FOUND));
        validationChain.validate(new FollowUpValidationContext(diagnosis, request));

        FollowUp followUp = FollowUp.builder()
                .diagnosis(diagnosis)
                .date(request.date())
                .scarType(scarType(request.scarTypeId()))
                .build();
        List<SegmentEvaluationRequest> evaluations = request.evaluations();
        for (int index = 0; index < evaluations.size(); index++) {
            addEvaluation(followUp, index, evaluations.get(index));
        }
        FollowUp saved = followUpRepository.saveAndFlush(followUp);
        // A new follow-up cannot be in a billing yet.
        return mapper.toResponse(saved, questionsLoader.questionsByTemplate(List.of(saved)), Set.of());
    }

    private void addEvaluation(FollowUp followUp, int index, SegmentEvaluationRequest request) {
        BodySegment segment = bodySegmentRepository.findById(request.segmentId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CATALOG_ITEM_NOT_FOUND));
        ClinicalMetrics metrics = new ClinicalMetrics(request.edema(), request.pain(), request.strength());
        TemplateAnswers answers = request.templateId() == null
                ? null
                : answerReader.read(request.segmentId(), request.templateId(), request.answersOrEmpty());
        SegmentEvaluation evaluation = followUp.evaluate(segment, metrics,
                answers == null ? null : answers.template());
        metricValueReader.read(index, request.segmentId(), request.metricsOrEmpty()).forEach(evaluation::record);
        if (answers != null) {
            answers.answers().forEach(evaluation::answer);
        }
    }

    private ScarType scarType(Long scarTypeId) {
        if (scarTypeId == null) {
            return null;
        }
        return scarTypeRepository.findById(scarTypeId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CATALOG_ITEM_NOT_FOUND));
    }
}
