package com.tomanagement.patient.service;

import java.time.Clock;
import java.time.Instant;

import com.tomanagement.common.exception.DuplicateResourceException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.ResourceNotFoundException;
import com.tomanagement.common.pagination.PageResponse;
import com.tomanagement.common.pagination.PaginationLimit;
import com.tomanagement.patient.dto.CreatePatientRequest;
import com.tomanagement.patient.dto.PatientResponse;
import com.tomanagement.patient.dto.UpdatePatientRequest;
import com.tomanagement.patient.entity.Patient;
import com.tomanagement.patient.repository.PatientRepository;
import com.tomanagement.patient.repository.PatientSearchSpecification;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientServiceImpl implements PatientService {

    private static final Sort BY_NAME = Sort.by(Sort.Order.asc("lastName").ignoreCase(),
            Sort.Order.asc("firstName").ignoreCase(), Sort.Order.asc("id"));

    private final PatientRepository patientRepository;
    private final Clock clock;

    public PatientServiceImpl(PatientRepository patientRepository, Clock clock) {
        this.patientRepository = patientRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PatientResponse> search(String query, int page, Integer size) {
        PageRequest request = PageRequest.of(Math.max(page, 0), PaginationLimit.clampPageSize(size), BY_NAME);
        return PageResponse.from(patientRepository.findAll(PatientSearchSpecification.matching(query), request),
                PatientResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponse get(Long patientId) {
        return PatientResponse.from(find(patientId));
    }

    @Override
    @Transactional
    public PatientResponse create(CreatePatientRequest request) {
        if (patientRepository.existsByDni(request.dni())) {
            throw new DuplicateResourceException(ErrorCode.PATIENT_DNI_DUPLICATE);
        }
        Patient patient = Patient.builder()
                .dni(request.dni())
                .data(request.data())
                .createdAt(Instant.now(clock))
                .build();
        try {
            return PatientResponse.from(patientRepository.saveAndFlush(patient));
        } catch (DataIntegrityViolationException raceOnUniqueDni) {
            throw new DuplicateResourceException(ErrorCode.PATIENT_DNI_DUPLICATE);
        }
    }

    @Override
    @Transactional
    public PatientResponse update(Long patientId, UpdatePatientRequest request) {
        Patient patient = find(patientId);
        patient.update(request.data());
        return PatientResponse.from(patient);
    }

    private Patient find(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PATIENT_NOT_FOUND));
    }
}
