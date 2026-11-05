package com.tomanagement.followup.validation;

import java.util.Objects;

import com.tomanagement.diagnosis.entity.Diagnosis;
import com.tomanagement.followup.dto.CreateFollowUpRequest;

/**
 * What every step of the {@link FollowUpValidationChain} looks at: the diagnosis being followed and the request.
 */
public record FollowUpValidationContext(Diagnosis diagnosis, CreateFollowUpRequest request) {

    public FollowUpValidationContext {
        Objects.requireNonNull(diagnosis, "diagnosis is required");
        Objects.requireNonNull(request, "request is required");
    }
}
