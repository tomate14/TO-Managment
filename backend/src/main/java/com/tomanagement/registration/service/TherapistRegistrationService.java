package com.tomanagement.registration.service;

import com.tomanagement.registration.dto.RegisterTherapistRequest;

public interface TherapistRegistrationService {

    /** Creates a pending, one-person practice whose ADMIN is the therapist. */
    void register(RegisterTherapistRequest request);
}
