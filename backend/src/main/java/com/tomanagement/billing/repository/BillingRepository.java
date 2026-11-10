package com.tomanagement.billing.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import com.tomanagement.billing.entity.Billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillingRepository extends JpaRepository<Billing, Long> {

    /**
     * Newest billing date first; both session origins remain in patient history.
     */
    List<Billing> findByPatient_IdOrderByBilledOnDescIdDesc(Long patientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Billing b where b.id = :id")
    Optional<Billing> findByIdForUpdate(@Param("id") Long id);
}
