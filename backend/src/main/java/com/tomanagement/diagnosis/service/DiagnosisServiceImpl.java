package com.tomanagement.diagnosis.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.ResourceNotFoundException;
import com.tomanagement.diagnosis.dto.CreateDiagnosisRequest;
import com.tomanagement.diagnosis.dto.DiagnosisResponse;
import com.tomanagement.diagnosis.entity.Diagnosis;
import com.tomanagement.diagnosis.mapper.DiagnosisMapper;
import com.tomanagement.diagnosis.repository.DiagnosisRepository;
import com.tomanagement.patient.entity.Patient;
import com.tomanagement.patient.repository.PatientRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiagnosisServiceImpl implements DiagnosisService {

    private final DiagnosisRepository diagnosisRepository;
    private final PatientRepository patientRepository;
    private final TreatmentFactory treatmentFactory;
    private final DiagnosisMapper mapper;
    private final FollowUpCounter followUpCounter;
    private final Clock clock;

    public DiagnosisServiceImpl(DiagnosisRepository diagnosisRepository, PatientRepository patientRepository,
            TreatmentFactory treatmentFactory, DiagnosisMapper mapper, FollowUpCounter followUpCounter, Clock clock) {
        this.diagnosisRepository = diagnosisRepository;
        this.patientRepository = patientRepository;
        this.treatmentFactory = treatmentFactory;
        this.mapper = mapper;
        this.followUpCounter = followUpCounter;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiagnosisResponse> listForPatient(Long patientId) {
        findPatient(patientId);
        List<Diagnosis> diagnoses = diagnosisRepository.findByPatient_IdOrderByCreatedDateDescIdDesc(patientId);
        Map<Long, Long> counts = followUpCounter.countByDiagnosis(diagnoses.stream().map(Diagnosis::getId).toList());
        return diagnoses.stream()
                .map(diagnosis -> mapper.toResponse(diagnosis, counts.getOrDefault(diagnosis.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional
    public DiagnosisResponse create(Long patientId, CreateDiagnosisRequest request) {
        Patient patient = findPatient(patientId);
        Diagnosis diagnosis = new Diagnosis(patient, request.description(), LocalDate.now(clock),
                treatmentFactory.create(request.treatment()));
        return mapper.toResponse(diagnosisRepository.saveAndFlush(diagnosis), 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public DiagnosisResponse get(Long diagnosisId) {
        return diagnosisRepository.findById(diagnosisId)
                .map(diagnosis -> mapper.toResponse(diagnosis,
                        followUpCounter.countByDiagnosis(List.of(diagnosisId)).getOrDefault(diagnosisId, 0L)))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DIAGNOSIS_NOT_FOUND));
    }

    private Patient findPatient(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PATIENT_NOT_FOUND));
    }
}
