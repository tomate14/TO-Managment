package com.tomanagement.followup.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.FieldValidationException;
import com.tomanagement.common.exception.dto.FieldErrorResponse;
import com.tomanagement.followup.dto.MetricValueRequest;
import com.tomanagement.segmentmetric.entity.SegmentMetric;
import com.tomanagement.segmentmetric.repository.SegmentMetricRepository;
import com.tomanagement.template.condition.AnswerValue;
import com.tomanagement.template.rules.AnswerTypeRulesFactory;

import org.springframework.stereotype.Component;

/**
 * Reads the values of the segment ROM metrics in one segment evaluation (É11): every value belongs to an active
 * metric of the segment and appears once, is valid for the metric type
 * ({@link com.tomanagement.template.rules.AnswerTypeRules}), and every required metric has a value. Value problems are
 * reported per metric ({@code evaluations[i].metrics[metricId]}). Used by the validation chain and, once valid, to
 * record the values.
 */
@Component
public class SegmentMetricValueReader {

    private final SegmentMetricRepository metricRepository;
    private final AnswerTypeRulesFactory rulesFactory;

    public SegmentMetricValueReader(SegmentMetricRepository metricRepository, AnswerTypeRulesFactory rulesFactory) {
        this.metricRepository = metricRepository;
        this.rulesFactory = rulesFactory;
    }

    /**
     * @param evaluationIndex position of the evaluation in the follow-up, for the field error paths
     * @return the typed values, in the metric display order
     * @throws BusinessRuleException    when a metric is not an active metric of the segment or is repeated
     * @throws FieldValidationException when a value is not valid or a required metric has no value
     */
    public Map<SegmentMetric, AnswerValue> read(int evaluationIndex, Long segmentId, List<MetricValueRequest> values) {
        List<SegmentMetric> metrics = metricRepository.findBySegment_IdAndActiveTrueOrderByDisplayOrderAsc(segmentId);
        Map<Long, SegmentMetric> metricsById = metrics.stream()
                .collect(Collectors.toMap(SegmentMetric::getId, Function.identity()));
        checkMetricsOfSegment(metricsById, values);

        List<FieldErrorResponse> errors = new ArrayList<>();
        Map<Long, AnswerValue> valuesById = new HashMap<>();
        for (MetricValueRequest value : values) {
            SegmentMetric metric = metricsById.get(value.metricId());
            Optional<AnswerValue> typed = rulesFactory.forType(metric.getAnswerType()).read(value.toRawAnswer(), metric);
            if (typed.isPresent()) {
                valuesById.put(metric.getId(), typed.get());
            } else {
                errors.add(fieldError(evaluationIndex, metric, MetricFieldMessage.INVALID));
            }
        }
        boolean missingRequired = false;
        for (SegmentMetric metric : metrics) {
            boolean sent = values.stream().anyMatch(value -> metric.getId().equals(value.metricId()));
            if (metric.isRequired() && !sent) {
                missingRequired = true;
                errors.add(fieldError(evaluationIndex, metric, MetricFieldMessage.REQUIRED));
            }
        }
        if (!errors.isEmpty()) {
            throw new FieldValidationException(
                    missingRequired ? ErrorCode.METRIC_VALUE_REQUIRED : ErrorCode.METRIC_VALUE_INVALID, errors);
        }

        Map<SegmentMetric, AnswerValue> byMetric = new LinkedHashMap<>();
        metrics.stream()
                .filter(metric -> valuesById.containsKey(metric.getId()))
                .forEach(metric -> byMetric.put(metric, valuesById.get(metric.getId())));
        return byMetric;
    }

    private static void checkMetricsOfSegment(Map<Long, SegmentMetric> metricsById, List<MetricValueRequest> values) {
        if (values.stream().anyMatch(value -> !metricsById.containsKey(value.metricId()))) {
            throw new BusinessRuleException(ErrorCode.METRIC_NOT_OF_SEGMENT);
        }
        Set<Long> seen = new HashSet<>();
        if (values.stream().anyMatch(value -> !seen.add(value.metricId()))) {
            throw new BusinessRuleException(ErrorCode.METRIC_VALUE_INVALID);
        }
    }

    private static FieldErrorResponse fieldError(int evaluationIndex, SegmentMetric metric,
            MetricFieldMessage message) {
        return new FieldErrorResponse("evaluations[" + evaluationIndex + "].metrics[" + metric.getId() + "]",
                message.getText());
    }
}
