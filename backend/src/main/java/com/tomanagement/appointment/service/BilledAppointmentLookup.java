package com.tomanagement.appointment.service;

import java.util.Collection;
import java.util.Set;

/** Port for agenda billing marks without depending on another feature's service. */
public interface BilledAppointmentLookup {

    Set<Long> billedAmong(Collection<Long> appointmentIds);
}
