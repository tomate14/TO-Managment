package com.tomanagement.patient.service;

import com.tomanagement.common.pagination.PageResponse;
import com.tomanagement.patient.dto.CreatePatientRequest;
import com.tomanagement.patient.dto.PatientResponse;
import com.tomanagement.patient.dto.UpdatePatientRequest;

/**
 * Patients, shared by every therapist (H4.1–H4.3).
 */
public interface PatientService {

    /**
     * Partial, case-insensitive search by DNI, first name or last name, ordered by last name.
     *
     * @param page zero-based
     * @param size clamped to the allowed page sizes
     */
    PageResponse<PatientResponse> search(String query, int page, Integer size);

    PatientResponse get(Long patientId);

    /**
     * @throws com.tomanagement.common.exception.DuplicateResourceException PATIENT_DNI_DUPLICATE
     */
    PatientResponse create(CreatePatientRequest request);

    PatientResponse update(Long patientId, UpdatePatientRequest request);
}
