package com.tomanagement.template.entity;

import java.util.Objects;

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
 * One choice of an OPTION question.
 */
@Entity
@Table(name = "question_option")
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "establishment_id", nullable = false, updatable = false)
    private Long establishmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false, length = 120)
    private String label;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected QuestionOption() {
        // Required by JPA.
    }

    QuestionOption(Question question, String label, int displayOrder) {
        this.question = Objects.requireNonNull(question, "question is required");
        relabel(label, displayOrder);
    }

    final void relabel(String label, int displayOrder) {
        this.label = Objects.requireNonNull(label, "label is required");
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
