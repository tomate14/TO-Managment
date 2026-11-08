package com.tomanagement.billing.repository;

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
}
