package com.tomanagement.followup.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.tomanagement.catalog.entity.BodySegment;
import com.tomanagement.segmentmetric.entity.SegmentMetric;
import com.tomanagement.template.condition.AnswerValue;
import com.tomanagement.template.entity.EvaluationTemplate;
import com.tomanagement.template.entity.Question;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.TenantId;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
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
 * The evaluation of one body segment within a follow-up (H6.2): fixed metrics, the values of the segment ROM metrics
 * (É11) and, optionally, the answers to one template of that segment (H6.3).
 */
@Entity
@Table(name = "segment_evaluation")
public class SegmentEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "establishment_id", nullable = false, updatable = false)
    private Long establishmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follow_up_id", nullable = false, updatable = false)
    private FollowUp followUp;

    @Column(name = "display_order", nullable = false, updatable = false)
    private int displayOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "body_segment_id", nullable = false, updatable = false)
    private BodySegment segment;

    @Embedded
    private ClinicalMetrics metrics;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", updatable = false)
    private EvaluationTemplate template;

    @OneToMany(mappedBy = "segmentEvaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @BatchSize(size = 50)
    private List<Answer> answers = new ArrayList<>();

    @OneToMany(mappedBy = "segmentEvaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @BatchSize(size = 50)
    private List<MetricValue> metricValues = new ArrayList<>();

    protected SegmentEvaluation() {
        // Required by JPA.
    }

    SegmentEvaluation(FollowUp followUp, int displayOrder, BodySegment segment, ClinicalMetrics metrics,
            EvaluationTemplate template) {
        this.followUp = Objects.requireNonNull(followUp, "followUp is required");
        this.displayOrder = displayOrder;
        this.segment = Objects.requireNonNull(segment, "segment is required");
        this.metrics = Objects.requireNonNull(metrics, "metrics is required");
        this.template = template;
    }

    /**
     * Records the answer to a question of this evaluation template (already validated by the caller).
     */
    public void answer(Question question, AnswerValue value) {
        answers.add(new Answer(this, question, value));
    }

    /**
     * Records the value of a ROM metric of this segment (already validated by the caller).
     */
    public void record(SegmentMetric metric, AnswerValue value) {
        metricValues.add(new MetricValue(this, metric, value));
    }

    public Long getId() {
        return id;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public BodySegment getSegment() {
        return segment;
    }

    public ClinicalMetrics getMetrics() {
        return metrics;
    }

    public Optional<EvaluationTemplate> getTemplate() {
        return Optional.ofNullable(template);
    }

    public List<Answer> getAnswers() {
        return List.copyOf(answers);
    }

    public List<MetricValue> getMetricValues() {
        return List.copyOf(metricValues);
    }
}
