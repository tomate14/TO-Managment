package com.tomanagement.billing.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.tomanagement.appointment.entity.Appointment;
import com.tomanagement.diagnosis.entity.Diagnosis;
import com.tomanagement.followup.entity.FollowUp;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Patient billing. Legacy diagnosis and follow-up associations are retained for historical records.
 */
@Entity
@Table(name = "billing")
public class Billing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "establishment_id", nullable = false, updatable = false)
    private Long establishmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false, updatable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnosis_id", updatable = false)
    private Diagnosis diagnosis;

    @Column(name = "billed_on", nullable = false)
    private LocalDate billedOn;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @OneToMany(mappedBy = "billing", cascade = CascadeType.ALL)
    private List<BillingFollowUp> links = new ArrayList<>();

    @OneToMany(mappedBy = "billing", cascade = CascadeType.ALL)
    private List<BillingAppointment> appointmentLinks = new ArrayList<>();

    protected Billing() {
        // Required by JPA.
    }

    public Billing(Diagnosis diagnosis, LocalDate billedOn, BigDecimal amount, Instant createdAt,
            List<FollowUp> followUps) {
        this.diagnosis = Objects.requireNonNull(diagnosis, "diagnosis is required");
        this.patient = diagnosis.getPatient();
        this.billedOn = Objects.requireNonNull(billedOn, "billedOn is required");
        this.amount = Objects.requireNonNull(amount, "amount is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        Objects.requireNonNull(followUps, "followUps is required")
                .forEach(followUp -> links.add(new BillingFollowUp(this, followUp)));
    }

    public Billing(Patient patient, LocalDate billedOn, BigDecimal amount, Instant createdAt,
            List<Appointment> appointments) {
        this.patient = Objects.requireNonNull(patient, "patient is required");
        this.billedOn = Objects.requireNonNull(billedOn, "billedOn is required");
        this.amount = Objects.requireNonNull(amount, "amount is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        Objects.requireNonNull(appointments, "appointments is required")
                .forEach(appointment -> appointmentLinks.add(new BillingAppointment(this, appointment)));
    }

    /**
     * Voids the billing and frees its sessions. Voiding again changes nothing.
     */
    public void voidBilling(Instant when) {
        if (voidedAt != null) {
            return;
        }
        this.voidedAt = Objects.requireNonNull(when, "when is required");
        links.forEach(BillingFollowUp::deactivate);
        appointmentLinks.forEach(BillingAppointment::deactivate);
    }

    public boolean isVoided() {
        return voidedAt != null;
    }

    public Long getId() {
        return id;
    }

    public Diagnosis getDiagnosis() {
        return diagnosis;
    }

    public Patient getPatient() {
        return patient;
    }

    public LocalDate getBilledOn() {
        return billedOn;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getVoidedAt() {
        return voidedAt;
    }

    public List<BillingFollowUp> getLinks() {
        return List.copyOf(links);
    }

    public List<FollowUp> getFollowUps() {
        return links.stream().map(BillingFollowUp::getFollowUp).toList();
    }

    public List<Appointment> getAppointments() {
        return appointmentLinks.stream().map(BillingAppointment::getAppointment).toList();
    }

    public BillingSessionOrigin getOrigin() {
        return diagnosis == null ? BillingSessionOrigin.APPOINTMENT : BillingSessionOrigin.LEGACY_FOLLOW_UP;
    }
}
