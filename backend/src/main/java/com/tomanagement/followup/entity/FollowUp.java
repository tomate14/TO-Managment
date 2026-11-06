package com.tomanagement.followup.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.tomanagement.catalog.entity.BodySegment;
import com.tomanagement.catalog.entity.ScarType;
import com.tomanagement.diagnosis.entity.Diagnosis;
import com.tomanagement.template.entity.EvaluationTemplate;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.TenantId;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
 * A follow-up of a diagnosis (spec §3.5, H6.1). Built with the Builder pattern; its segment evaluations are added
 * with {@link #evaluate} and saved with it in one operation.
 */
@Entity
@Table(name = "follow_up")
public class FollowUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "establishment_id", nullable = false, updatable = false)
    private Long establishmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diagnosis_id", nullable = false, updatable = false)
    private Diagnosis diagnosis;

    @Column(name = "follow_up_date", nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scar_type_id")
    private ScarType scarType;

    @OneToMany(mappedBy = "followUp", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @BatchSize(size = 50)
    private List<SegmentEvaluation> evaluations = new ArrayList<>();

    protected FollowUp() {
        // Required by JPA.
    }

    private FollowUp(Builder builder) {
        this.diagnosis = Objects.requireNonNull(builder.diagnosis, "diagnosis is required");
        this.date = Objects.requireNonNull(builder.date, "date is required");
        this.scarType = builder.scarType;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Adds the evaluation of a segment, numbered in the order it is added.
     */
    public SegmentEvaluation evaluate(BodySegment segment, ClinicalMetrics metrics, EvaluationTemplate template) {
        SegmentEvaluation evaluation = new SegmentEvaluation(this, evaluations.size(), segment, metrics, template);
        evaluations.add(evaluation);
        return evaluation;
    }

    public Long getId() {
        return id;
    }

    public Diagnosis getDiagnosis() {
        return diagnosis;
    }

    public LocalDate getDate() {
        return date;
    }

    public Optional<ScarType> getScarType() {
        return Optional.ofNullable(scarType);
    }

    public List<SegmentEvaluation> getEvaluations() {
        return List.copyOf(evaluations);
    }

    public static final class Builder {

        private Diagnosis diagnosis;
        private LocalDate date;
        private ScarType scarType;

        private Builder() {
        }

        public Builder diagnosis(Diagnosis diagnosis) {
            this.diagnosis = diagnosis;
            return this;
        }

        public Builder date(LocalDate date) {
            this.date = date;
            return this;
        }

        /**
         * @param scarType required for surgical treatments, null for conservative ones (checked before building)
         */
        public Builder scarType(ScarType scarType) {
            this.scarType = scarType;
            return this;
        }

        public FollowUp build() {
            return new FollowUp(this);
        }
    }
}
