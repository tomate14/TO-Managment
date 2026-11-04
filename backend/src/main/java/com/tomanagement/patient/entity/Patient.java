package com.tomanagement.patient.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A patient (spec §3.3). Built with the Builder pattern; the DNI is fixed once created.
 */
@Entity
@Table(name = "patient")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 12, updatable = false)
    private String dni;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 40)
    private String phone;

    @Column(nullable = false, length = 120)
    private String occupation;

    @Column(name = "health_insurance", nullable = false, length = 120)
    private String healthInsurance;

    @Column(name = "health_insurance_number", nullable = false, length = 60)
    private String healthInsuranceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Dominance dominance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Patient() {
        // Required by JPA.
    }

    private Patient(Builder builder) {
        this.dni = Objects.requireNonNull(Dni.normalize(builder.dni), "dni is required");
        this.createdAt = Objects.requireNonNull(builder.createdAt, "createdAt is required");
        update(Objects.requireNonNull(builder.data, "data is required"));
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Replaces every field except the DNI (H4.3).
     */
    public final void update(PatientData data) {
        this.firstName = Objects.requireNonNull(data.firstName(), "firstName is required");
        this.lastName = Objects.requireNonNull(data.lastName(), "lastName is required");
        this.birthDate = Objects.requireNonNull(data.birthDate(), "birthDate is required");
        this.phone = Objects.requireNonNull(data.phone(), "phone is required");
        this.occupation = Objects.requireNonNull(data.occupation(), "occupation is required");
        this.healthInsurance = Objects.requireNonNull(data.healthInsurance(), "healthInsurance is required");
        this.healthInsuranceNumber = Objects.requireNonNull(data.healthInsuranceNumber(), "healthInsuranceNumber is required");
        this.dominance = Objects.requireNonNull(data.dominance(), "dominance is required");
    }

    public Long getId() {
        return id;
    }

    public String getDni() {
        return dni;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getPhone() {
        return phone;
    }

    public String getOccupation() {
        return occupation;
    }

    public String getHealthInsurance() {
        return healthInsurance;
    }

    public String getHealthInsuranceNumber() {
        return healthInsuranceNumber;
    }

    public Dominance getDominance() {
        return dominance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public static final class Builder {

        private String dni;
        private PatientData data;
        private Instant createdAt;

        private Builder() {
        }

        public Builder dni(String dni) {
            this.dni = dni;
            return this;
        }

        public Builder data(PatientData data) {
            this.data = data;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Patient build() {
            return new Patient(this);
        }
    }
}
