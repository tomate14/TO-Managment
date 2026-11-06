package com.tomanagement.appointment.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import com.tomanagement.appointment.entity.AppointmentDuration;
import com.tomanagement.appointment.repository.AppointmentRepository;
import com.tomanagement.appointment.visibility.AdminVisibilityPolicy;
import com.tomanagement.appointment.visibility.AppointmentVisibilityPolicyFactory;
import com.tomanagement.appointment.visibility.TherapistVisibilityPolicy;
import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.common.exception.AccessDeniedApplicationException;
import com.tomanagement.common.exception.ApplicationException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.diagnosis.entity.Diagnosis;
import com.tomanagement.patient.entity.Dominance;
import com.tomanagement.patient.entity.Patient;
import com.tomanagement.patient.entity.PatientData;
import com.tomanagement.user.entity.Role;
import com.tomanagement.user.entity.User;
import com.tomanagement.diagnosis.entity.Treatment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

class AppointmentValidationChainTest {

    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.parse("2026-11-05T11:40").atZone(ZONE).toInstant(), ZONE);

    private final AppointmentRepository repository = mock(AppointmentRepository.class);
    private AppointmentValidationChain chain;
    private User therapist;
    private Patient lucia;
    private Patient diego;
    private AuthenticatedUser admin;

    @BeforeEach
    void setUp() {
        AppointmentVisibilityPolicyFactory policies = new AppointmentVisibilityPolicyFactory(
                List.of(new AdminVisibilityPolicy(), new TherapistVisibilityPolicy()));
        chain = new AppointmentValidationChain(new ScheduleStep(CLOCK), new PermissionStep(policies),
                new TherapistActiveStep(), new DiagnosisOwnershipStep(), new TherapistFreeStep(repository),
                new PatientFreeStep(repository));
        therapist = user(2L, true);
        lucia = patient(10L, "Lucía");
        diego = patient(11L, "Diego");
        admin = new AuthenticatedUser(1L, "admin@example.com", "Admin", Role.ADMIN, null);
    }

    @ParameterizedTest
    @CsvSource({"2026-11-08T10:00,30", "2026-11-10T07:30,30", "2026-11-10T19:30,60", "2026-11-10T10:15,30"})
    void rejectsStartsOutsideTheAgenda(String start, int minutes) {
        assertError(draft(LocalDateTime.parse(start), minutes), ErrorCode.APPOINTMENT_OUTSIDE_HOURS);
    }

    @Test
    void acceptsTheLastSlotThatEndsAtClosing() {
        assertValid(draft(LocalDateTime.parse("2026-11-10T19:00"), 60));
    }

    @Test
    void rejectsThePast() {
        assertError(draft(LocalDateTime.parse("2026-11-05T11:30"), 30), ErrorCode.APPOINTMENT_IN_PAST);
    }

    @Test
    void backToBackIsNotABusyTherapist() {
        when(repository.existsOverlapForTherapist(any(), any(), any(), any())).thenReturn(false);
        assertValid(draft(LocalDateTime.parse("2026-11-10T10:30"), 30));
    }

    @Test
    void busyTherapistAndBusyPatient() {
        when(repository.existsOverlapForTherapist(any(), any(), any(), any())).thenReturn(true);
        assertError(draft(LocalDateTime.parse("2026-11-10T10:30"), 30), ErrorCode.THERAPIST_BUSY);
        when(repository.existsOverlapForTherapist(any(), any(), any(), any())).thenReturn(false);
        when(repository.existsOverlapForPatient(any(), any(), any(), any())).thenReturn(true);
        assertError(draft(LocalDateTime.parse("2026-11-10T10:30"), 30), ErrorCode.PATIENT_BUSY);
    }

    @Test
    void diagnosisOfAnotherPatient() {
        Diagnosis ofDiego = new Diagnosis(diego, "Lesión", LocalDate.of(2026, 10, 1), mock(Treatment.class));
        AppointmentDraft draft = new AppointmentDraft(therapist, lucia, ofDiego,
                LocalDateTime.parse("2026-11-10T10:30"), AppointmentDuration.MINUTES_30, null);
        assertError(draft, ErrorCode.DIAGNOSIS_NOT_OF_PATIENT);
    }

    @Test
    void inactiveTherapist() {
        therapist.changeActive(false);
        assertError(draft(LocalDateTime.parse("2026-11-10T10:30"), 30), ErrorCode.THERAPIST_INACTIVE);
    }

    @Test
    void aTherapistCannotBookAnotherTherapistsAgenda() {
        AuthenticatedUser other = new AuthenticatedUser(99L, "other@example.com", "Other", Role.TERAPEUTA, null);
        AppointmentValidationContext context =
                new AppointmentValidationContext(other, draft(LocalDateTime.parse("2026-11-10T10:30"), 30));
        assertThatThrownBy(() -> chain.validate(context)).isInstanceOf(AccessDeniedApplicationException.class);
    }

    private AppointmentDraft draft(LocalDateTime start, int minutes) {
        return new AppointmentDraft(therapist, lucia, null, start,
                AppointmentDuration.fromMinutes(minutes).orElseThrow(), null);
    }

    private void assertValid(AppointmentDraft draft) {
        assertThatCode(() -> chain.validate(new AppointmentValidationContext(admin, draft)))
                .doesNotThrowAnyException();
    }

    private void assertError(AppointmentDraft draft, ErrorCode code) {
        assertThatThrownBy(() -> chain.validate(new AppointmentValidationContext(admin, draft)))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode")
                .isEqualTo(code);
        assertThat(code).isNotNull();
    }

    private static User user(Long id, boolean active) {
        User user = User.builder().email("therapist@example.com").passwordHash("hash").fullName("Therapist")
                .role(Role.TERAPEUTA).active(active).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static Patient patient(Long id, String firstName) {
        Patient patient = Patient.builder()
                .dni("3000000" + id)
                .data(new PatientData(firstName, "Fernández", LocalDate.of(1984, 3, 21), "249 412-3456", "Docente",
                        "OSDE", "61 234567 8 01", Dominance.RIGHT))
                .createdAt(Instant.parse("2026-11-04T12:00:00Z"))
                .build();
        ReflectionTestUtils.setField(patient, "id", id);
        return patient;
    }
}
