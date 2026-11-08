package com.tomanagement.followup.entity;

import java.math.BigDecimal;
import java.util.Objects;

import com.tomanagement.segmentmetric.entity.SegmentMetric;
import com.tomanagement.template.condition.AnswerValue;

import org.hibernate.annotations.TenantId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * The value of one segment ROM metric in a segment evaluation (É11). One column per value type; only the column of
 * the metric type is filled.
 */
@Entity
@Table(name = "metric_value")
public class MetricValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "establishment_id", nullable = false, updatable = false)
    private Long establishmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "segment_evaluation_id", nullable = false, updatable = false)
    private SegmentEvaluation segmentEvaluation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "segment_metric_id", nullable = false, updatable = false)
    private SegmentMetric metric;

    @Column(name = "numeric_value", precision = 12, scale = 3)
    private BigDecimal numericValue;

    @Column(name = "boolean_value")
    private Boolean booleanValue;

    @Column(name = "option_value", length = 120)
    private String optionValue;

    @Column(name = "text_value", columnDefinition = "text")
    private String textValue;

    protected MetricValue() {
        // Required by JPA.
    }

    MetricValue(SegmentEvaluation segmentEvaluation, SegmentMetric metric, AnswerValue value) {
        this.segmentEvaluation = Objects.requireNonNull(segmentEvaluation, "segmentEvaluation is required");
        this.metric = Objects.requireNonNull(metric, "metric is required");
        Objects.requireNonNull(value, "value is required");
        switch (value.getType()) {
            case NUMERIC -> this.numericValue = value.number().orElseThrow();
            case YES_NO -> this.booleanValue = value.yesNo().orElseThrow();
            case OPTION -> this.optionValue = value.text().orElseThrow();
            case TEXT -> this.textValue = value.text().orElseThrow();
        }
    }

    public Long getId() {
        return id;
    }

    public SegmentMetric getMetric() {
        return metric;
    }

    public BigDecimal getNumericValue() {
        return numericValue;
    }

    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public String getOptionValue() {
        return optionValue;
    }

    public String getTextValue() {
        return textValue;
    }
}
