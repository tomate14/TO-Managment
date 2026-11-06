package com.tomanagement.appointment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.tomanagement.TestTable;
import com.tomanagement.TestTables;
import com.tomanagement.TestTenancy;
import com.tomanagement.TestcontainersConfiguration;
import com.tomanagement.appointment.entity.Appointment;
import com.tomanagement.appointment.entity.AppointmentDuration;
import com.tomanagement.appointment.service.OverlapConstraint;
import com.tomanagement.patient.entity.Dominance;
import com.tomanagement.patient.entity.Patient;
import com.tomanagement.patient.entity.PatientData;
import com.tomanagement.patient.repository.PatientRepository;
import com.tomanagement.user.entity.Role;
import com.tomanagement.user.entity.User;
import com.tomanagement.user.repository.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/**
 * Appointment persistence and the database overlap guarantees (É7), against a real Postgres. Repository calls run
 * under the test establishment (outside a request the tenant would otherwise be NONE).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestTables.class, TestTenancy.class})
class AppointmentRepositoryTest {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private TestTables testTables;

    @Autowired
    private TestTenancy tenancy;

    private Long establishmentId;
    private User therapist;
    private Patient lucia;
    private Patient diego;

    @BeforeEach
    void seed() {
        testTables.truncate(TestTable.APPOINTMENT, TestTable.FOLLOW_UP, TestTable.DIAGNOSIS, TestTable.TREATMENT,
                TestTable.PATIENT, TestTable.APP_USER, TestTable.ESTABLISHMENT);
        establishmentId = tenancy.create("Consultorio").getId();
        therapist = userRepository.save(User.builder()
                .email("ana@consultorio.com")
                .passwordHash("x".repeat(60))
                .fullName("Ana Terapeuta")
                .role(Role.TERAPEUTA)
                .active(true)
                .establishmentId(establishmentId)
                .createdAt(Instant.parse("2026-11-04T12:00:00Z"))
                .build());
        tenancy.runAs(establishmentId, () -> {
            lucia = patient("30123456", "Lucía");
            diego = patient("28999888", "Diego");
        });
    }

    @AfterEach
    void cleanUp() {
        testTables.truncate(TestTable.APPOINTMENT, TestTable.FOLLOW_UP, TestTable.DIAGNOSIS, TestTable.TREATMENT, TestTable.PATIENT);
    }

    @Test
    void theDatabaseRejectsOverlappingAppointmentsOfTheSameTherapist() {
        tenancy.runAs(establishmentId, () -> {
            appointmentRepository.saveAndFlush(appointment(therapist, lucia, at("2026-11-10T10:00"), AppointmentDuration.MINUTES_60));
            assertThatThrownBy(() -> appointmentRepository.saveAndFlush(
                    appointment(therapist, diego, at("2026-11-10T10:30"), AppointmentDuration.MINUTES_30)))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining(OverlapConstraint.THERAPIST.getConstraintName());
        });
    }

    @Test
    void backToBackAppointmentsAreAllowed() {
        tenancy.runAs(establishmentId, () -> {
            appointmentRepository.saveAndFlush(appointment(therapist, lucia, at("2026-11-10T10:00"), AppointmentDuration.MINUTES_30));
            appointmentRepository.saveAndFlush(appointment(therapist, diego, at("2026-11-10T10:30"), AppointmentDuration.MINUTES_30));
            assertThat(appointmentRepository.count()).isEqualTo(2);
        });
    }

    @Test
    void overlapQueriesIgnoreTheExcludedAppointment() {
        tenancy.runAs(establishmentId, () -> {
            Appointment saved = appointmentRepository.saveAndFlush(appointment(therapist, lucia, at("2026-11-10T10:00"), AppointmentDuration.MINUTES_60));
            assertThat(appointmentRepository.existsOverlapForTherapist(therapist.getId(), at("2026-11-10T10:30"), at("2026-11-10T11:00"), null)).isTrue();
            assertThat(appointmentRepository.existsOverlapForTherapist(therapist.getId(), at("2026-11-10T10:30"), at("2026-11-10T11:00"), saved.getId())).isFalse();
            assertThat(appointmentRepository.existsOverlapForPatient(lucia.getId(), at("2026-11-10T11:00"), at("2026-11-10T11:30"), null)).isFalse();
        });
    }

    private Patient patient(String dni, String firstName) {
        return patientRepository.save(Patient.builder()
                .dni(dni)
                .data(new PatientData(firstName, "Fernández", LocalDate.of(1984, 3, 21), "249 412-3456", "Docente", "OSDE",
                        "61 234567 8 01", Dominance.RIGHT))
                .createdAt(Instant.parse("2026-11-04T12:00:00Z"))
                .build());
    }

    private static Appointment appointment(User therapist, Patient patient, LocalDateTime startAt, AppointmentDuration duration) {
        return new Appointment(therapist, patient, null, startAt, duration, null, Instant.parse("2026-11-04T12:00:00Z"));
    }

    private static LocalDateTime at(String isoLocal) {
        return LocalDateTime.parse(isoLocal);
    }
}
