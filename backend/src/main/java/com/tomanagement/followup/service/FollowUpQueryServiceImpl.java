package com.tomanagement.followup.service;

import java.util.List;
import java.util.Map;

import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.ResourceNotFoundException;
import com.tomanagement.diagnosis.repository.DiagnosisRepository;
import com.tomanagement.followup.dto.FollowUpResponse;
import com.tomanagement.followup.entity.FollowUp;
import com.tomanagement.followup.mapper.FollowUpMapper;
import com.tomanagement.followup.repository.FollowUpRepository;
import com.tomanagement.template.entity.Question;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowUpQueryServiceImpl implements FollowUpQueryService {

    private final DiagnosisRepository diagnosisRepository;
    private final FollowUpRepository followUpRepository;
    private final TemplateQuestionsLoader questionsLoader;
    private final FollowUpMapper mapper;

    public FollowUpQueryServiceImpl(DiagnosisRepository diagnosisRepository, FollowUpRepository followUpRepository,
            TemplateQuestionsLoader questionsLoader, FollowUpMapper mapper) {
        this.diagnosisRepository = diagnosisRepository;
        this.followUpRepository = followUpRepository;
        this.questionsLoader = questionsLoader;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FollowUpResponse> listForDiagnosis(Long diagnosisId) {
        if (!diagnosisRepository.existsById(diagnosisId)) {
            throw new ResourceNotFoundException(ErrorCode.DIAGNOSIS_NOT_FOUND);
        }
        List<FollowUp> followUps = followUpRepository.findByDiagnosis_IdOrderByDateDescIdDesc(diagnosisId);
        Map<Long, List<Question>> questionsByTemplate = questionsLoader.questionsByTemplate(followUps);
        return followUps.stream()
                .map(followUp -> mapper.toResponse(followUp, questionsByTemplate))
                .toList();
    }
}
