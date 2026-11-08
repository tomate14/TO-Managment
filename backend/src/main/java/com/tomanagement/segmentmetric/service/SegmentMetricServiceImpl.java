package com.tomanagement.segmentmetric.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.tomanagement.catalog.entity.BodySegment;
import com.tomanagement.catalog.repository.BodySegmentRepository;
import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.DuplicateResourceException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.FieldValidationException;
import com.tomanagement.common.exception.ResourceNotFoundException;
import com.tomanagement.common.exception.dto.FieldErrorResponse;
import com.tomanagement.segmentmetric.dto.SegmentMetricCountResponse;
import com.tomanagement.segmentmetric.dto.SegmentMetricRequest;
import com.tomanagement.segmentmetric.dto.SegmentMetricResponse;
import com.tomanagement.segmentmetric.entity.SegmentMetric;
import com.tomanagement.segmentmetric.repository.SegmentMetricRepository;
import com.tomanagement.template.entity.AnswerType;
import com.tomanagement.template.rules.QuestionLimit;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SegmentMetricServiceImpl implements SegmentMetricService {

    private final SegmentMetricRepository metricRepository;
    private final BodySegmentRepository segmentRepository;

    public SegmentMetricServiceImpl(SegmentMetricRepository metricRepository,
            BodySegmentRepository segmentRepository) {
        this.metricRepository = metricRepository;
        this.segmentRepository = segmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SegmentMetricResponse> list(Long segmentId) {
        findSegment(segmentId);
        return metricRepository.findBySegment_IdOrderByDisplayOrderAsc(segmentId).stream()
                .map(SegmentMetricResponse::from).toList();
    }

    @Override
    @Transactional
    public SegmentMetricResponse create(Long segmentId, SegmentMetricRequest request) {
        BodySegment segment = findSegment(segmentId);
        AnswerType answerType = request.answerType();
        if (answerType == null) {
            throw new FieldValidationException(ErrorCode.VALIDATION_ERROR,
                    List.of(new FieldErrorResponse("answerType", "Elegí el tipo de la métrica.")));
        }
        List<String> options = optionsFor(answerType, request);
        if (metricRepository.existsBySegment_IdAndNameIgnoreCase(segmentId, request.name())) {
            throw new DuplicateResourceException(ErrorCode.SEGMENT_METRIC_NAME_TAKEN);
        }
        int displayOrder = metricRepository.maxDisplayOrder(segmentId) + 1;
        return save(new SegmentMetric(segment, request.name(), answerType, request.required(), displayOrder, options));
    }

    @Override
    @Transactional
    public SegmentMetricResponse update(Long segmentId, Long id, SegmentMetricRequest request) {
        SegmentMetric metric = findMetric(segmentId, id);
        List<String> options = optionsFor(metric.getAnswerType(), request);
        if (metricRepository.existsBySegment_IdAndNameIgnoreCaseAndIdNot(segmentId, request.name(), id)) {
            throw new DuplicateResourceException(ErrorCode.SEGMENT_METRIC_NAME_TAKEN);
        }
        metric.rename(request.name());
        metric.changeRequired(request.required());
        metric.replaceOptions(options);
        return save(metric);
    }

    @Override
    @Transactional
    public SegmentMetricResponse changeActive(Long segmentId, Long id, boolean active) {
        SegmentMetric metric = findMetric(segmentId, id);
        metric.changeActive(active);
        return SegmentMetricResponse.from(metric);
    }

    @Override
    @Transactional
    public List<SegmentMetricResponse> reorder(Long segmentId, List<Long> ids) {
        findSegment(segmentId);
        List<SegmentMetric> metrics = metricRepository.findBySegment_IdOrderByDisplayOrderAsc(segmentId);
        Set<Long> expected = metrics.stream().map(SegmentMetric::getId).collect(Collectors.toSet());
        if (ids.size() != expected.size() || !expected.equals(new HashSet<>(ids))) {
            throw new BusinessRuleException(ErrorCode.SEGMENT_METRIC_ORDER_INVALID);
        }
        for (SegmentMetric metric : metrics) {
            metric.moveTo(ids.indexOf(metric.getId()) + 1);
        }
        return metrics.stream()
                .sorted(Comparator.comparingInt(SegmentMetric::getDisplayOrder))
                .map(SegmentMetricResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SegmentMetricCountResponse> counts() {
        return metricRepository.countActiveBySegment();
    }

    private SegmentMetricResponse save(SegmentMetric metric) {
        try {
            return SegmentMetricResponse.from(metricRepository.saveAndFlush(metric));
        } catch (DataIntegrityViolationException raceOnUniqueName) {
            throw new DuplicateResourceException(ErrorCode.SEGMENT_METRIC_NAME_TAKEN);
        }
    }

    private BodySegment findSegment(Long segmentId) {
        return segmentRepository.findById(segmentId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CATALOG_ITEM_NOT_FOUND));
    }

    private SegmentMetric findMetric(Long segmentId, Long id) {
        findSegment(segmentId);
        return metricRepository.findById(id)
                .filter(metric -> metric.getSegment().getId().equals(segmentId))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SEGMENT_METRIC_NOT_FOUND));
    }

    /** Options only apply to OPTION metrics: other types ignore them. OPTION needs 2+ distinct labels. */
    private static List<String> optionsFor(AnswerType answerType, SegmentMetricRequest request) {
        if (answerType != AnswerType.OPTION) {
            return List.of();
        }
        List<String> options = request.options();
        Set<String> distinct = options.stream().map(option -> option.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (options.size() < QuestionLimit.MINIMUM_OPTIONS.getValue() || distinct.size() != options.size()) {
            throw new BusinessRuleException(ErrorCode.SEGMENT_METRIC_OPTIONS_INVALID);
        }
        return options;
    }
}
