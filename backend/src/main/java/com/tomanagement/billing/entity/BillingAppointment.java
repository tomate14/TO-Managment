package com.tomanagement.billing.entity;

import java.util.Objects;

import com.tomanagement.appointment.entity.Appointment;

import org.hibernate.annotations.TenantId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A retained appointment link; only an active link counts toward current billing. */
@Entity
@Table(name = "billing_appointment")
@IdClass(BillingAppointmentId.class)
public class BillingAppointment {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billing_id", nullable = false, updatable = false)
    private Billing billing;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, updatable = false)
    private Appointment appointment;

    @TenantId
    @Column(name = "establishment_id", nullable = false, updatable = false)
    private Long establishmentId;

    @Column(nullable = false)
    private boolean active;

    protected BillingAppointment() {
        // Required by JPA.
    }

    BillingAppointment(Billing billing, Appointment appointment) {
        this.billing = Objects.requireNonNull(billing, "billing is required");
        this.appointment = Objects.requireNonNull(appointment, "appointment is required");
        this.active = true;
    }

    void deactivate() {
        active = false;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public boolean isActive() {
        return active;
    }
}
