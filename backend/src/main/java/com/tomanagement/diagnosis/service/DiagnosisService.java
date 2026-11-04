package com.tomanagement.diagnosis.service;

import java.util.List;

import com.tomanagement.diagnosis.dto.CreateDiagnosisRequest;
import com.tomanagement.diagnosis.dto.DiagnosisResponse;

/**
 * Diagnoses of a patient (H5.1, H5.2). There is no edition: the treatment type is fixed at creation.
 */
public interface DiagnosisService {

    /**
     * Newest first.
     *
     * @throws com.tomanagement.common.exception.ResourceNotFoundException PATIENT_NOT_FOUND
     */
    List<DiagnosisResponse> listForPatient(Long patientId);

    /**
     * The creation date is today in APP_TIME_ZONE.
     */
    DiagnosisResponse create(Long patientId, CreateDiagnosisRequest request);

    DiagnosisResponse get(Long diagnosisId);
}
