package com.tomanagement.followup.entity;

import java.math.BigDecimal;
import java.util.Objects;

import com.tomanagement.template.condition.AnswerValue;
import com.tomanagement.template.entity.Question;

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
 * The answer to one template question in a segment evaluation (H6.3). One column per value type; only the column of
 * the question type is filled.
 */
@Entity
@Table(name = "answer")
public class Answer {

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
    @JoinColumn(name = "question_id", nullable = false, updatable = false)
    private Question question;

    @Column(name = "numeric_value", precision = 12, scale = 3)
    private BigDecimal numericValue;

    @Column(name = "boolean_value")
    private Boolean booleanValue;

    @Column(name = "option_value", length = 120)
    private String optionValue;

    @Column(name = "text_value", columnDefinition = "text")
    private String textValue;

    protected Answer() {
        // Required by JPA.
    }

    Answer(SegmentEvaluation segmentEvaluation, Question question, AnswerValue value) {
        this.segmentEvaluation = Objects.requireNonNull(segmentEvaluation, "segmentEvaluation is required");
        this.question = Objects.requireNonNull(question, "question is required");
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

    public Question getQuestion() {
        return question;
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
