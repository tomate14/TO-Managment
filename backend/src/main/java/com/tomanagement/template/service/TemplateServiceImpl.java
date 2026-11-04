package com.tomanagement.template.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.tomanagement.catalog.entity.BodySegment;
import com.tomanagement.catalog.repository.BodySegmentRepository;
import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.DuplicateResourceException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.ResourceNotFoundException;
import com.tomanagement.template.dto.TemplateDetailResponse;
import com.tomanagement.template.dto.TemplateRequest;
import com.tomanagement.template.dto.TemplateSummaryResponse;
import com.tomanagement.template.entity.EvaluationTemplate;
import com.tomanagement.template.entity.Question;
import com.tomanagement.template.mapper.TemplateMapper;
import com.tomanagement.template.repository.EvaluationTemplateRepository;
import com.tomanagement.template.repository.QuestionRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateServiceImpl implements TemplateService {

    private final EvaluationTemplateRepository templateRepository;
    private final QuestionRepository questionRepository;
    private final BodySegmentRepository segmentRepository;
    private final TemplateMapper mapper;

    public TemplateServiceImpl(EvaluationTemplateRepository templateRepository, QuestionRepository questionRepository,
            BodySegmentRepository segmentRepository, TemplateMapper mapper) {
        this.templateRepository = templateRepository;
        this.questionRepository = questionRepository;
        this.segmentRepository = segmentRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateSummaryResponse> list(Long segmentId) {
        List<EvaluationTemplate> templates = segmentId == null
                ? templateRepository.findAllByOrderByNameAsc()
                : templateRepository.findBySegment_IdOrderByNameAsc(segmentId);
        Map<Long, List<Question>> questionsByTemplate = questionRepository
                .findByTemplate_IdIn(templates.stream().map(EvaluationTemplate::getId).toList()).stream()
                .collect(Collectors.groupingBy(question -> question.getTemplate().getId()));
        return templates.stream()
                .map(template -> mapper.toSummary(template, questionsByTemplate.getOrDefault(template.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateDetailResponse get(Long templateId) {
        return detail(find(templateId));
    }

    @Override
    @Transactional
    public TemplateDetailResponse create(TemplateRequest request) {
        BodySegment segment = activeSegment(request.segmentId());
        if (templateRepository.existsBySegment_IdAndNameIgnoreCase(segment.getId(), request.name())) {
            throw new DuplicateResourceException(ErrorCode.TEMPLATE_DUPLICATE);
        }
        return detail(save(new EvaluationTemplate(request.name(), segment)));
    }

    @Override
    @Transactional
    public TemplateDetailResponse update(Long templateId, TemplateRequest request) {
        EvaluationTemplate template = find(templateId);
        BodySegment segment = template.getSegment().getId().equals(request.segmentId())
                ? template.getSegment()
                : activeSegment(request.segmentId());
        if (templateRepository.existsBySegment_IdAndNameIgnoreCaseAndIdNot(segment.getId(), request.name(), templateId)) {
            throw new DuplicateResourceException(ErrorCode.TEMPLATE_DUPLICATE);
        }
        template.update(request.name(), segment);
        return detail(save(template));
    }

    @Override
    @Transactional
    public TemplateDetailResponse changeActive(Long templateId, boolean active) {
        EvaluationTemplate template = find(templateId);
        template.changeActive(active);
        return detail(template);
    }

    private EvaluationTemplate save(EvaluationTemplate template) {
        try {
            return templateRepository.saveAndFlush(template);
        } catch (DataIntegrityViolationException raceOnUniqueName) {
            throw new DuplicateResourceException(ErrorCode.TEMPLATE_DUPLICATE);
        }
    }

    private TemplateDetailResponse detail(EvaluationTemplate template) {
        return mapper.toDetail(template, questionRepository.findByTemplate_IdOrderByDisplayOrderAsc(template.getId()));
    }

    private BodySegment activeSegment(Long segmentId) {
        BodySegment segment = segmentRepository.findById(segmentId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CATALOG_ITEM_NOT_FOUND));
        if (!segment.isActive()) {
            throw new BusinessRuleException(ErrorCode.SEGMENT_INACTIVE);
        }
        return segment;
    }

    private EvaluationTemplate find(Long templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TEMPLATE_NOT_FOUND));
    }
}
