package com.tomanagement.diagnosis.entity;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * State pattern: the treatment decides the rules that depend on it. Each subtype validates its own data when it
 * is built and answers whether follow-ups need a scar type (É6). Persisted with JOINED inheritance; callers that
 * need subtype data use {@link #accept(TreatmentVisitor)} instead of casts.
 */
@Entity
@Table(name = "treatment")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "treatment_type", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class Treatment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    protected Treatment() {
        // Required by JPA.
    }

    public Long getId() {
        return id;
    }

    public abstract TreatmentType getType();

    /**
     * Surgical treatments leave a scar: their follow-ups must record its type; conservative ones must not.
     */
    public abstract boolean requiresScarType();

    public abstract <R> R accept(TreatmentVisitor<R> visitor);
}
