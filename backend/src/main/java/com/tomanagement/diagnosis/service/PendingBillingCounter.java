package com.tomanagement.diagnosis.service;

import java.util.Collection;
import java.util.Map;

/**
 * Port: pending (unbilled) follow-ups per diagnosis, implemented by the billing feature so the diagnosis package does
 * not depend on it.
 */
public interface PendingBillingCounter {

    /**
     * @return pending follow-ups per diagnosis id; diagnoses without pending ones are missing
     */
    Map<Long, Long> pendingByDiagnosis(Collection<Long> diagnosisIds);
}
