package com.tomanagement.template.entity;

import java.util.Objects;

import com.tomanagement.catalog.entity.BodySegment;

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
 * Reusable set of questions for one body segment (H3.1). Deactivated instead of deleted.
 */
@Entity
@Table(name = "evaluation_template")
public class EvaluationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "body_segment_id", nullable = false)
    private BodySegment segment;

    @Column(nullable = false)
    private boolean active;

    protected EvaluationTemplate() {
        // Required by JPA.
    }

    public EvaluationTemplate(String name, BodySegment segment) {
        update(name, segment);
        this.active = true;
    }

    public final void update(String name, BodySegment segment) {
        this.name = Objects.requireNonNull(name, "name is required");
        this.segment = Objects.requireNonNull(segment, "segment is required");
    }

    public void changeActive(boolean active) {
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BodySegment getSegment() {
        return segment;
    }

    public boolean isActive() {
        return active;
    }
}
