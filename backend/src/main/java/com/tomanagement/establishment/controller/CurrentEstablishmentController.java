package com.tomanagement.establishment.controller;

import com.tomanagement.establishment.dto.CurrentEstablishmentResponse;
import com.tomanagement.establishment.dto.RenameEstablishmentRequest;
import com.tomanagement.establishment.service.CurrentEstablishmentService;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The establishment of the current session; an establishment is required (guarded by the tenant interceptor).
 */
@RestController
@RequestMapping("/api/establishment")
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
public class CurrentEstablishmentController {

    private final CurrentEstablishmentService currentEstablishmentService;

    public CurrentEstablishmentController(CurrentEstablishmentService currentEstablishmentService) {
        this.currentEstablishmentService = currentEstablishmentService;
    }

    @GetMapping
    public CurrentEstablishmentResponse get() {
        return currentEstablishmentService.get();
    }

    @PutMapping
    public CurrentEstablishmentResponse rename(@Valid @RequestBody RenameEstablishmentRequest request) {
        return currentEstablishmentService.rename(request);
    }
}
