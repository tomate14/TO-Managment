package com.tomanagement.appointment.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.tomanagement.appointment.entity.Appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Appointments. Overlap checks use half-open ranges, so back-to-back appointments do not overlap.
 */
public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {

    @Query("select count(a) > 0 from Appointment a where a.therapist.id = :therapistId "
            + "and a.startAt < :end and a.endAt > :start and (:excludeId is null or a.id <> :excludeId)")
    boolean existsOverlapForTherapist(@Param("therapistId") Long therapistId, @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end, @Param("excludeId") Long excludeId);

    @Query("select count(a) > 0 from Appointment a where a.patient.id = :patientId "
            + "and a.startAt < :end and a.endAt > :start and (:excludeId is null or a.id <> :excludeId)")
    boolean existsOverlapForPatient(@Param("patientId") Long patientId, @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end, @Param("excludeId") Long excludeId);

    @Query("select a from Appointment a where (a.therapist.id = :therapistId or a.patient.id = :patientId) "
            + "and a.startAt < :dayEnd and a.endAt > :dayStart and (:excludeId is null or a.id <> :excludeId)")
    List<Appointment> findOverlappingForTherapistOrPatient(@Param("therapistId") Long therapistId,
            @Param("patientId") Long patientId, @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd, @Param("excludeId") Long excludeId);
}
