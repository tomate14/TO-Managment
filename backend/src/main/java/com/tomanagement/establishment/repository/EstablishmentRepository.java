package com.tomanagement.establishment.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.tomanagement.establishment.entity.Establishment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstablishmentRepository extends JpaRepository<Establishment, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<Establishment> findAllByOrderByNameAsc();

    /** Trial end of an establishment; empty when it has no expiry or does not exist. */
    @Query("select e.trialEndsAt from Establishment e where e.id = :id")
    Optional<Instant> findTrialEndsAtById(@Param("id") Long id);
}
