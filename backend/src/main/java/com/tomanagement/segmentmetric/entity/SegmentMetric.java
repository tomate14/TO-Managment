package com.tomanagement.segmentmetric.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.tomanagement.catalog.entity.BodySegment;
import com.tomanagement.template.entity.AnswerType;
import com.tomanagement.template.rules.AnswerConstraints;

import org.hibernate.annotations.TenantId;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * A ROM metric an establishment defines for a body segment (H11.1). The answer type never changes after creation;
 * options are edited in place to keep their ids stable. Metrics have no numeric range.
 */
@Entity
@Table(name = "segment_metric")
public class SegmentMetric implements AnswerConstraints {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "establishment_id", nullable = false, updatable = false)
    private Long establishmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "body_segment_id", nullable = false, updatable = false)
    private BodySegment segment;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_type", nullable = false, updatable = false, length = 20)
    private AnswerType answerType;

    @Column(nullable = false)
    private boolean required;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @OneToMany(mappedBy = "metric", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<SegmentMetricOption> options = new ArrayList<>();

    protected SegmentMetric() {
        // Required by JPA.
    }

    public SegmentMetric(BodySegment segment, String name, AnswerType answerType, boolean required, int displayOrder,
            List<String> optionLabels) {
        this.segment = Objects.requireNonNull(segment, "segment is required");
        this.name = Objects.requireNonNull(name, "name is required");
        this.answerType = Objects.requireNonNull(answerType, "answerType is required");
        this.required = required;
        this.displayOrder = displayOrder;
        this.active = true;
        replaceOptions(optionLabels);
    }

    public void rename(String name) {
        this.name = Objects.requireNonNull(name, "name is required");
    }

    public void changeRequired(boolean required) {
        this.required = required;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }

    public void moveTo(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    /**
     * Relabels the existing options by position, appends the new ones and drops the tail.
     */
    public void replaceOptions(List<String> labels) {
        for (int index = 0; index < labels.size(); index++) {
            if (index < options.size()) {
                options.get(index).relabel(labels.get(index), index);
            } else {
                options.add(new SegmentMetricOption(this, labels.get(index), index));
            }
        }
        while (options.size() > labels.size()) {
            options.remove(options.size() - 1);
        }
    }

    public Long getId() {
        return id;
    }

    public BodySegment getSegment() {
        return segment;
    }

    public String getName() {
        return name;
    }

    public AnswerType getAnswerType() {
        return answerType;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isActive() {
        return active;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    @Override
    public BigDecimal getMinValue() {
        return null;
    }

    @Override
    public BigDecimal getMaxValue() {
        return null;
    }

    @Override
    public List<String> getOptionLabels() {
        return options.stream().map(SegmentMetricOption::getLabel).toList();
    }
}
