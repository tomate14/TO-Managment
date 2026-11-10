package com.tomanagement.billing.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.tomanagement.appointment.service.BilledAppointmentLookup;
import com.tomanagement.billing.repository.BillingAppointmentRepository;

import org.springframework.stereotype.Service;

/** Repository adapter keeps the agenda and billing services independent. */
@Service
public class BilledAppointmentAdapter implements BilledAppointmentLookup {

    private final BillingAppointmentRepository linkRepository;

    public BilledAppointmentAdapter(BillingAppointmentRepository linkRepository) {
        this.linkRepository = linkRepository;
    }

    @Override
    public Set<Long> billedAmong(Collection<Long> appointmentIds) {
        return appointmentIds.isEmpty() ? Set.of() : new HashSet<>(linkRepository.findBilledAmong(appointmentIds));
    }
}
