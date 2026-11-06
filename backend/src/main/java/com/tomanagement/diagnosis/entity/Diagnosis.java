package com.tomanagement.diagnosis.entity;

import java.time.LocalDate;
import java.util.Objects;

import com.tomanagement.patient.entity.Patient;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * A patient's diagnosis (spec §3.4). The creation date is assigned by the system and the treatment never changes.
 */
@Entity
@Table(name = "diagnosis")
public class Diagnosis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "establishment_id", nullable = false, updatable = false)
    private Long establishmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false, updatable = false)
    private Patient patient;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDate createdDate;

    /** Eager so the concrete treatment subtype is loaded (a lazy proxy would only be the abstract Treatment). */
    @OneToOne(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "treatment_id", nullable = false, unique = true, updatable = false)
    private Treatment treatment;

    protected Diagnosis() {
        // Required by JPA.
    }

    public Diagnosis(Patient patient, String description, LocalDate createdDate, Treatment treatment) {
        this.patient = Objects.requireNonNull(patient, "patient is required");
        this.description = Objects.requireNonNull(description, "description is required");
        this.createdDate = Objects.requireNonNull(createdDate, "createdDate is required");
        this.treatment = Objects.requireNonNull(treatment, "treatment is required");
    }

    public Long getId() {
        return id;
    }

    public Patient getPatient() {
        return patient;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public Treatment getTreatment() {
        return treatment;
    }
}
