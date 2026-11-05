package com.tomanagement.appointment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.tomanagement.appointment.dto.CreateAppointmentRequest;
import com.tomanagement.appointment.entity.Appointment;
import com.tomanagement.appointment.mapper.AppointmentMapper;
import com.tomanagement.appointment.repository.AppointmentRepository;
import com.tomanagement.appointment.validation.AppointmentValidationChain;
import com.tomanagement.appointment.visibility.AdminVisibilityPolicy;
import com.tomanagement.appointment.visibility.AppointmentVisibilityPolicyFactory;
import com.tomanagement.appointment.visibility.TherapistVisibilityPolicy;
import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.common.exception.ApplicationException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.diagnosis.repository.DiagnosisRepository;
import com.tomanagement.patient.entity.Dominance;
import com.tomanagement.patient.entity.Patient;
import com.tomanagement.patient.entity.PatientData;
import com.tomanagement.patient.repository.PatientRepository;
import com.tomanagement.user.entity.Role;
import com.tomanagement.user.entity.User;
import com.tomanagement.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

class AppointmentServiceImplTest {

    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1L, "admin@example.com", "Admin", Role.ADMIN);

    private final AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final DiagnosisRepository diagnosisRepository = mock(DiagnosisRepository.class);
    private final AppointmentValidationChain chain = mock(AppointmentValidationChain.class);
    private AppointmentServiceImpl service;
    private CreateAppointmentRequest request;

    @BeforeEach
    void setUp() {
        AppointmentVisibilityPolicyFactory policies = new AppointmentVisibilityPolicyFactory(
                List.of(new AdminVisibilityPolicy(), new TherapistVisibilityPolicy()));
        service = new AppointmentServiceImpl(appointmentRepository, userRepository, patientRepository,
                diagnosisRepository, chain, policies, new AppointmentMapper(),
                Clock.fixed(Instant.parse("2026-11-05T14:40:00Z"), ZoneOffset.UTC));
        User therapist = User.builder().email("carla@example.com").passwordHash("x".repeat(60)).fullName("Carla")
                .role(Role.TERAPEUTA).active(true).createdAt(Instant.parse("2026-11-04T12:00:00Z")).build();
        ReflectionTestUtils.setField(therapist, "id", 2L);
        Patient patient = Patient.builder().dni("30123456")
                .data(new PatientData("Lucía", "Fernández", LocalDate.of(1984, 3, 21), "249 412-3456", "Docente",
                        "OSDE", "61 234567 8 01", Dominance.RIGHT))
                .createdAt(Instant.parse("2026-11-04T12:00:00Z")).build();
        ReflectionTestUtils.setField(patient, "id", 7L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(therapist));
        when(patientRepository.findById(7L)).thenReturn(Optional.of(patient));
        request = new CreateAppointmentRequest(2L, 7L, null, LocalDate.of(2026, 11, 10), LocalTime.of(12, 0), 45, null);
    }

    @ParameterizedTest
    @CsvSource({"ex_appointment_therapist_overlap,THERAPIST_BUSY", "ex_appointment_patient_overlap,PATIENT_BUSY"})
    void exclusionViolationsBecomeConflicts(String constraint, ErrorCode expected) {
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenThrow(new DataIntegrityViolationException(
                "ERROR: conflicting key value violates exclusion constraint \"" + constraint + "\""));
        assertThatThrownBy(() -> service.book(ADMIN, request))
                .isInstanceOf(ApplicationException.class).extracting("errorCode").isEqualTo(expected);
    }
}
