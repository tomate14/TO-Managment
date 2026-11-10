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
     * Ids, among the given follow-ups, of the ones in a standing billing.
     */
    @Query("select l.followUp.id from BillingFollowUp l where l.active = true and l.followUp.id in :followUpIds")
    List<Long> findBilledAmong(@Param("followUpIds") Collection<Long> followUpIds);

}
