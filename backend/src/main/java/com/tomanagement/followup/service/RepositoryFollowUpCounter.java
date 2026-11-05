package com.tomanagement.followup.service;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import com.tomanagement.diagnosis.service.FollowUpCounter;
import com.tomanagement.followup.repository.FollowUpRepository;
import com.tomanagement.followup.repository.FollowUpRepository.DiagnosisFollowUpCount;

import org.springframework.stereotype.Component;

@Component
public class RepositoryFollowUpCounter implements FollowUpCounter {

    private final FollowUpRepository followUpRepository;

    public RepositoryFollowUpCounter(FollowUpRepository followUpRepository) {
        this.followUpRepository = followUpRepository;
    }

    @Override
    public Map<Long, Long> countByDiagnosis(Collection<Long> diagnosisIds) {
        if (diagnosisIds.isEmpty()) {
            return Map.of();
        }
        return followUpRepository.countByDiagnosisIds(diagnosisIds).stream()
                .collect(Collectors.toMap(DiagnosisFollowUpCount::getDiagnosisId, DiagnosisFollowUpCount::getTotal));
    }
}
