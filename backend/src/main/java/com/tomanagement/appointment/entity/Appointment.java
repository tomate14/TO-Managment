package com.tomanagement.appointment.entity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import com.tomanagement.diagnosis.entity.Diagnosis;
import com.tomanagement.patient.entity.Patient;
import com.tomanagement.user.entity.User;

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
 * A scheduled appointment of a patient with a therapist (E7). The patient never changes once booked.
 */
@Entity
@Table(name = "appointment")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "therapist_id", nullable = false)
    private User therapist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false, updatable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnosis_id")
    private Diagnosis diagnosis;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Appointment() {
        // Required by JPA.
    }

    public Appointment(User therapist, Patient patient, Diagnosis diagnosis, LocalDateTime startAt,
            AppointmentDuration duration, String notes, Instant createdAt) {
        this.patient = Objects.requireNonNull(patient, "patient is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        reschedule(therapist, diagnosis, startAt, duration, notes);
    }

    public final void reschedule(User therapist, Diagnosis diagnosis, LocalDateTime startAt,
            AppointmentDuration duration, String notes) {
        this.therapist = Objects.requireNonNull(therapist, "therapist is required");
        this.diagnosis = diagnosis;
        this.startAt = Objects.requireNonNull(startAt, "startAt is required");
        this.endAt = startAt.plusMinutes(Objects.requireNonNull(duration, "duration is required").getMinutes());
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public User getTherapist() {
        return therapist;
    }

    public Patient getPatient() {
        return patient;
    }

    public Optional<Diagnosis> getDiagnosis() {
        return Optional.ofNullable(diagnosis);
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public Optional<String> getNotes() {
        return Optional.ofNullable(notes);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
