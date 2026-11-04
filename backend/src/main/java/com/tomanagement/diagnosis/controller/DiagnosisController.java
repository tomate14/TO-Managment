package com.tomanagement.diagnosis.controller;

import java.util.List;

import com.tomanagement.diagnosis.dto.CreateDiagnosisRequest;
import com.tomanagement.diagnosis.dto.DiagnosisResponse;
import com.tomanagement.diagnosis.service.DiagnosisService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Diagnoses: listed and created under their patient, read by id.
 */
@RestController
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    public DiagnosisController(DiagnosisService diagnosisService) {
        this.diagnosisService = diagnosisService;
    }

    @GetMapping("/api/patients/{patientId}/diagnoses")
    public List<DiagnosisResponse> list(@PathVariable("patientId") Long patientId) {
        return diagnosisService.listForPatient(patientId);
    }

    @PostMapping("/api/patients/{patientId}/diagnoses")
    @ResponseStatus(HttpStatus.CREATED)
    public DiagnosisResponse create(@PathVariable("patientId") Long patientId,
            @Valid @RequestBody CreateDiagnosisRequest request) {
        return diagnosisService.create(patientId, request);
    }

    @GetMapping("/api/diagnoses/{id}")
    public DiagnosisResponse get(@PathVariable("id") Long id) {
        return diagnosisService.get(id);
    }
}
