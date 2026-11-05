package com.tomanagement.followup.repository;

import java.util.Collection;
import java.util.List;

import com.tomanagement.followup.entity.FollowUp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {

    /**
     * Newest first (H6.4); the id breaks ties between follow-ups of the same day.
     */
    List<FollowUp> findByDiagnosis_IdOrderByDateDescIdDesc(Long diagnosisId);

    @Query("select f.diagnosis.id as diagnosisId, count(f) as total from FollowUp f "
            + "where f.diagnosis.id in :diagnosisIds group by f.diagnosis.id")
    List<DiagnosisFollowUpCount> countByDiagnosisIds(@Param("diagnosisIds") Collection<Long> diagnosisIds);

    /**
     * Projection of {@link #countByDiagnosisIds}.
     */
    interface DiagnosisFollowUpCount {

        Long getDiagnosisId();

        long getTotal();
    }
}
