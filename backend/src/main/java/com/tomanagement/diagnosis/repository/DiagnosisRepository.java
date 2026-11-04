package com.tomanagement.diagnosis.repository;

import java.util.List;

import com.tomanagement.diagnosis.entity.Diagnosis;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {

    /**
     * Newest first (H5.2); the id breaks ties between diagnoses created the same day.
     */
    List<Diagnosis> findByPatient_IdOrderByCreatedDateDescIdDesc(Long patientId);
}
