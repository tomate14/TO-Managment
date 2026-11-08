package com.tomanagement.billing.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.tomanagement.billing.repository.BillingFollowUpRepository;
import com.tomanagement.billing.repository.BillingFollowUpRepository.DiagnosisPendingCount;
import com.tomanagement.diagnosis.service.PendingBillingCounter;
import com.tomanagement.followup.service.BilledFollowUpLookup;

import org.springframework.stereotype.Service;

/**
 * Adapter: exposes the billing state to the diagnosis and follow-up features through their ports. It reads only
 * repositories, so it creates no bean cycle with the services that use it.
 */
@Service
public class BillingCounterAdapter implements PendingBillingCounter, BilledFollowUpLookup {

    private final BillingFollowUpRepository linkRepository;

    public BillingCounterAdapter(BillingFollowUpRepository linkRepository) {
        this.linkRepository = linkRepository;
    }

    @Override
    public Map<Long, Long> pendingByDiagnosis(Collection<Long> diagnosisIds) {
        if (diagnosisIds.isEmpty()) {
            return Map.of();
        }
        return linkRepository.countPendingByDiagnosisIds(diagnosisIds).stream()
                .collect(Collectors.toMap(DiagnosisPendingCount::getDiagnosisId, DiagnosisPendingCount::getTotal));
    }

    @Override
    public Set<Long> billedAmong(Collection<Long> followUpIds) {
        if (followUpIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(linkRepository.findBilledAmong(followUpIds));
    }
}
