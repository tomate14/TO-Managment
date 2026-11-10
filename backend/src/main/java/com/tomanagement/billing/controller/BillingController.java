package com.tomanagement.billing.controller;

import com.tomanagement.billing.dto.BillingSummaryResponse;
import com.tomanagement.billing.dto.CreateBillingRequest;
import com.tomanagement.billing.service.BillingService;

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
 * Patient appointment billing; available to every signed-in user.
 */
@RestController
@RequestMapping("/api")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/patients/{patientId}/billing")
    public BillingSummaryResponse summary(@PathVariable("patientId") Long patientId) {
        return billingService.summary(patientId);
    }

    @PostMapping("/patients/{patientId}/billings")
    @ResponseStatus(HttpStatus.CREATED)
    public BillingSummaryResponse register(@PathVariable("patientId") Long patientId,
            @Valid @RequestBody CreateBillingRequest request) {
        return billingService.register(patientId, request);
    }

    @PostMapping("/billings/{id}/void")
    public BillingSummaryResponse voidBilling(@PathVariable("id") Long id) {
        return billingService.voidBilling(id);
    }
}
