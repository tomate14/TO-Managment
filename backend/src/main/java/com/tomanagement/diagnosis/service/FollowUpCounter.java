package com.tomanagement.diagnosis.service;

import java.util.Collection;
import java.util.Map;

/**
 * Counts the follow-ups of diagnoses (shown on the diagnosis cards). Implemented by the follow-up feature, so the
 * diagnosis package does not depend on it.
 */
public interface FollowUpCounter {

    /**
     * @return follow-ups per diagnosis id; diagnoses without follow-ups may be missing
     */
    Map<Long, Long> countByDiagnosis(Collection<Long> diagnosisIds);
}
