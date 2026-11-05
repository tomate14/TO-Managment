package com.tomanagement.followup.controller;

import java.util.List;

import com.tomanagement.followup.dto.CreateFollowUpRequest;
import com.tomanagement.followup.dto.FollowUpResponse;
import com.tomanagement.followup.service.FollowUpQueryService;
import com.tomanagement.followup.service.FollowUpRegistrationService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Follow-ups of a diagnosis (É6); available to every signed-in user.
 */
@RestController
@RequestMapping("/api/diagnoses/{diagnosisId}/follow-ups")
public class FollowUpController {

    private final FollowUpQueryService queryService;
    private final FollowUpRegistrationService registrationService;

    public FollowUpController(FollowUpQueryService queryService, FollowUpRegistrationService registrationService) {
        this.queryService = queryService;
        this.registrationService = registrationService;
    }

    @GetMapping
    public List<FollowUpResponse> list(@PathVariable("diagnosisId") Long diagnosisId) {
        return queryService.listForDiagnosis(diagnosisId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FollowUpResponse register(@PathVariable("diagnosisId") Long diagnosisId,
            @Valid @RequestBody CreateFollowUpRequest request) {
        return registrationService.register(diagnosisId, request);
    }
}
