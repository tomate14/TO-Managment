package com.tomanagement.billing.repository;

import java.util.Collection;
import java.util.List;

import com.tomanagement.billing.entity.BillingFollowUp;
import com.tomanagement.billing.entity.BillingFollowUpId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillingFollowUpRepository extends JpaRepository<BillingFollowUp, BillingFollowUpId> {

    /**
     * Ids of the follow-ups of a diagnosis that are in a standing billing.
     */
    @Query("select l.followUp.id from BillingFollowUp l where l.active = true and l.billing.diagnosis.id = :diagnosisId")
    List<Long> findBilledFollowUpIds(@Param("diagnosisId") Long diagnosisId);

    /**
     * Ids, among the given follow-ups, of the ones in a standing billing.
     */
    @Query("select l.followUp.id from BillingFollowUp l where l.active = true and l.followUp.id in :followUpIds")
    List<Long> findBilledAmong(@Param("followUpIds") Collection<Long> followUpIds);

    /**
     * Follow-ups per diagnosis that are not in a standing billing; diagnoses without any are missing.
     */
    @Query("select f.diagnosis.id as diagnosisId, count(f) as total from FollowUp f "
            + "where f.diagnosis.id in :diagnosisIds and not exists "
            + "(select 1 from BillingFollowUp l where l.active = true and l.followUp = f) "
            + "group by f.diagnosis.id")
    List<DiagnosisPendingCount> countPendingByDiagnosisIds(@Param("diagnosisIds") Collection<Long> diagnosisIds);

    /**
     * Projection of {@link #countPendingByDiagnosisIds}.
     */
    interface DiagnosisPendingCount {

        Long getDiagnosisId();

        Long getTotal();
    }
}
