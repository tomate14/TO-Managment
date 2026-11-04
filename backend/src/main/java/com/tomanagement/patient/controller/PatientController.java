package com.tomanagement.patient.controller;

import com.tomanagement.common.pagination.PageResponse;
import com.tomanagement.patient.dto.CreatePatientRequest;
import com.tomanagement.patient.dto.PatientResponse;
import com.tomanagement.patient.dto.UpdatePatientRequest;
import com.tomanagement.patient.service.PatientService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Patients; every signed-in user sees the same patients.
 */
@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public PageResponse<PatientResponse> search(@RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", required = false) Integer size) {
        return patientService.search(query, page, size);
    }

    @GetMapping("/{id}")
    public PatientResponse get(@PathVariable("id") Long id) {
        return patientService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientResponse create(@Valid @RequestBody CreatePatientRequest request) {
        return patientService.create(request);
    }

    @PutMapping("/{id}")
    public PatientResponse update(@PathVariable("id") Long id, @Valid @RequestBody UpdatePatientRequest request) {
        return patientService.update(id, request);
    }
}
