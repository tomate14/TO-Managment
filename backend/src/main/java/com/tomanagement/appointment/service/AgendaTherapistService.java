package com.tomanagement.appointment.service;

import java.util.List;

import com.tomanagement.appointment.dto.AgendaTherapistResponse;
import com.tomanagement.auth.security.AuthenticatedUser;

public interface AgendaTherapistService {

    List<AgendaTherapistResponse> list(AuthenticatedUser actor);
}
