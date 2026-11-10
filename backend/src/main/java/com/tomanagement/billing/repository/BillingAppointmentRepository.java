package com.tomanagement.billing.repository;

import java.util.Collection;
import java.util.List;

import com.tomanagement.billing.entity.BillingAppointment;
import com.tomanagement.billing.entity.BillingAppointmentId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillingAppointmentRepository extends JpaRepository<BillingAppointment, BillingAppointmentId> {

    @Query("select l.appointment.id from BillingAppointment l where l.active = true "
            + "and l.appointment.id in :appointmentIds")
    List<Long> findBilledAmong(@Param("appointmentIds") Collection<Long> appointmentIds);
}
