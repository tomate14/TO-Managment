package com.tomanagement.billing.entity;

import java.io.Serializable;
import java.util.Objects;

/** Association identifiers are represented by their primary key types in an IdClass. */
public class BillingAppointmentId implements Serializable {

    private Long billing;
    private Long appointment;

    public BillingAppointmentId() {
        // Required by JPA.
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BillingAppointmentId that)) {
            return false;
        }
        return Objects.equals(billing, that.billing) && Objects.equals(appointment, that.appointment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(billing, appointment);
    }
}
