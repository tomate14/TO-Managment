package com.tomanagement.billing.repository;

import java.util.List;

import com.tomanagement.billing.entity.Billing;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingRepository extends JpaRepository<Billing, Long> {

    /**
     * Newest billing date first; the id breaks ties. Loads the links with them.
     */
    @EntityGraph(attributePaths = "links")
    List<Billing> findByDiagnosis_IdOrderByBilledOnDescIdDesc(Long diagnosisId);
}
