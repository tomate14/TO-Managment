package com.tomanagement.followup.service;

import com.tomanagement.followup.dto.CreateFollowUpRequest;
import com.tomanagement.followup.dto.FollowUpResponse;

/**
 * Facade (spec §4): registers a whole follow-up (H6.1–H6.3) — validation, mapping and persistence — in one
 * transaction.
 */
public interface FollowUpRegistrationService {

    FollowUpResponse register(Long diagnosisId, CreateFollowUpRequest request);
}
