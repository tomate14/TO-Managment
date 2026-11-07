package com.tomanagement.establishment.service;

import java.util.List;

import com.tomanagement.establishment.dto.CreateEstablishmentRequest;
import com.tomanagement.establishment.dto.EstablishmentSummaryResponse;
import com.tomanagement.establishment.dto.RenameEstablishmentRequest;

/**
 * Establishment management for the superadmin.
 */
public interface EstablishmentService {

    List<EstablishmentSummaryResponse> list();

    EstablishmentSummaryResponse create(CreateEstablishmentRequest request);

    /** Approves a pending practice; idempotent, and it also reactivates a rejected or deactivated one. */
    EstablishmentSummaryResponse approve(Long id);

    EstablishmentSummaryResponse rename(Long id, RenameEstablishmentRequest request);

    EstablishmentSummaryResponse changeActive(Long id, boolean active);
}
