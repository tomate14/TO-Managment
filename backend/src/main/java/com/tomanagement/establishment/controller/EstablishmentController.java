package com.tomanagement.establishment.controller;

import java.util.List;

import com.tomanagement.establishment.dto.ChangeEstablishmentActiveRequest;
import com.tomanagement.establishment.dto.CreateEstablishmentRequest;
import com.tomanagement.establishment.dto.EstablishmentSummaryResponse;
import com.tomanagement.establishment.dto.RenameEstablishmentRequest;
import com.tomanagement.establishment.service.EstablishmentService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Establishment management, superadmin only.
 */
@RestController
@RequestMapping("/api/establishments")
@PreAuthorize("hasRole('SUPERADMIN')")
public class EstablishmentController {

    private final EstablishmentService establishmentService;

    public EstablishmentController(EstablishmentService establishmentService) {
        this.establishmentService = establishmentService;
    }

    @GetMapping
    public List<EstablishmentSummaryResponse> list() {
        return establishmentService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EstablishmentSummaryResponse create(@Valid @RequestBody CreateEstablishmentRequest request) {
        return establishmentService.create(request);
    }

    @PutMapping("/{id}")
    public EstablishmentSummaryResponse rename(@PathVariable("id") Long id,
            @Valid @RequestBody RenameEstablishmentRequest request) {
        return establishmentService.rename(id, request);
    }

    @PostMapping("/{id}/approval")
    public EstablishmentSummaryResponse approve(@PathVariable("id") Long id) {
        return establishmentService.approve(id);
    }

    @PatchMapping("/{id}/active")
    public EstablishmentSummaryResponse changeActive(@PathVariable("id") Long id,
            @Valid @RequestBody ChangeEstablishmentActiveRequest request) {
        return establishmentService.changeActive(id, request.active());
    }
}
