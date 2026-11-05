package com.tomanagement.followup.service;

import java.util.List;

import com.tomanagement.followup.dto.FollowUpResponse;

/**
 * The follow-up history of a diagnosis (H6.4).
 */
public interface FollowUpQueryService {

    /**
     * Newest first, with evaluations and answers.
     */
    List<FollowUpResponse> listForDiagnosis(Long diagnosisId);
}
