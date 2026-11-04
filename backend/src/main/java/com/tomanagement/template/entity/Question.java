package com.tomanagement.template.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
 * A template question (H3.2). Optionally chained to an earlier question of the same template through an
 * activation condition (H3.3). Options are edited in place to keep their ids stable.
 */
@Entity
@Table(name = "question")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private EvaluationTemplate template;

    @Column(nullable = false, length = 255)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_type", nullable = false, length = 20)
    private AnswerType answerType;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "min_value", precision = 12, scale = 3)
    private BigDecimal minValue;

    @Column(name = "max_value", precision = 12, scale = 3)
    private BigDecimal maxValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_question_id")
    private Question parent;

    @Embedded
    private ActivationConditionData condition;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<QuestionOption> options = new ArrayList<>();

    protected Question() {
        // Required by JPA.
    }

    public Question(EvaluationTemplate template, int displayOrder) {
        this.template = Objects.requireNonNull(template, "template is required");
        this.displayOrder = displayOrder;
        this.active = true;
    }

    /**
     * Replaces the whole definition. Range and options must already be cleared for types that do not use them.
     */
    public void define(QuestionDefinition definition) {
        this.text = Objects.requireNonNull(definition.text(), "text is required");
        this.answerType = Objects.requireNonNull(definition.answerType(), "answerType is required");
        this.minValue = definition.minValue();
        this.maxValue = definition.maxValue();
        this.parent = definition.parent();
        this.condition = definition.condition();
        replaceOptions(definition.optionLabels());
    }

    public void changeActive(boolean active) {
        this.active = active;
    }

    private void replaceOptions(List<String> labels) {
        for (int index = 0; index < labels.size(); index++) {
            if (index < options.size()) {
                options.get(index).relabel(labels.get(index), index);
            } else {
                options.add(new QuestionOption(this, labels.get(index), index));
            }
        }
        while (options.size() > labels.size()) {
            options.remove(options.size() - 1);
        }
    }

    public Long getId() {
        return id;
    }

    public EvaluationTemplate getTemplate() {
        return template;
    }

    public String getText() {
        return text;
    }

    public AnswerType getAnswerType() {
        return answerType;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isActive() {
        return active;
    }

    public BigDecimal getMinValue() {
        return minValue;
    }

    public BigDecimal getMaxValue() {
        return maxValue;
    }

    public Optional<Question> getParent() {
        return Optional.ofNullable(parent);
    }

    public Optional<ActivationConditionData> getCondition() {
        return Optional.ofNullable(condition);
    }

    public List<String> getOptionLabels() {
        return options.stream().map(QuestionOption::getLabel).toList();
    }
}
