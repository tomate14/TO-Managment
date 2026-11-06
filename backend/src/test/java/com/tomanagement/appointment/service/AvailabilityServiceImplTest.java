package com.tomanagement.appointment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import com.tomanagement.appointment.dto.SlotResponse;
import com.tomanagement.appointment.dto.SlotUnavailableReason;
import com.tomanagement.appointment.entity.Appointment;
import com.tomanagement.appointment.entity.AppointmentDuration;
import com.tomanagement.appointment.repository.AppointmentRepository;
import com.tomanagement.appointment.visibility.AdminVisibilityPolicy;
import com.tomanagement.appointment.visibility.AppointmentVisibilityPolicyFactory;
import com.tomanagement.appointment.visibility.TherapistVisibilityPolicy;
import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.patient.entity.Dominance;
import com.tomanagement.patient.entity.Patient;
import com.tomanagement.patient.entity.PatientData;
import com.tomanagement.user.entity.Role;
import com.tomanagement.user.entity.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AvailabilityServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Clock CLOCK =
            Clock.fixed(LocalDateTime.parse("2026-11-05T11:40").atZone(ZONE).toInstant(), ZONE);
    private static final AuthenticatedUser ADMIN =
            new AuthenticatedUser(1L, "admin@example.com", "Admin", Role.ADMIN, null);

    private final AppointmentRepository repository = mock(AppointmentRepository.class);
    private AvailabilityServiceImpl service;
    private User therapist2;
    private User therapist3;
    private Patient patient7;
    private Patient otherPatient;

    @BeforeEach
    void setUp() {
        AppointmentVisibilityPolicyFactory policies = new AppointmentVisibilityPolicyFactory(
                List.of(new AdminVisibilityPolicy(), new TherapistVisibilityPolicy()));
        service = new AvailabilityServiceImpl(repository, policies, CLOCK);
        therapist2 = user(2L);
        therapist3 = user(3L);
        patient7 = patient(7L);
        otherPatient = patient(8L);
    }

    @Test
    void listsStartsEveryHalfHourSoTheLastOneEndsAtClosing() {
        List<SlotResponse> slots = service.slots(ADMIN, 2L, null, LocalDate.parse("2026-11-10"),
                AppointmentDuration.MINUTES_45, null);
        assertThat(slots).extracting(SlotResponse::startTime).first().isEqualTo(LocalTime.of(8, 0));
        assertThat(slots).extracting(SlotResponse::startTime).last().isEqualTo(LocalTime.of(19, 0));
        assertThat(slots).hasSize(23);
    }

    @Test
    void sundaysHaveNoSlots() {
        assertThat(service.slots(ADMIN, 2L, null, LocalDate.parse("2026-11-08"), AppointmentDuration.MINUTES_30,
                null)).isEmpty();
    }

    @Test
    void marksPastBusyTherapistAndBusyPatient() {
        when(repository.findOverlappingForTherapistOrPatient(eq(2L), eq(7L), any(), any(), isNull()))
                .thenReturn(List.of(appointment(therapist2, otherPatient, "2026-11-05T12:00", 45),
                        appointment(therapist3, patient7, "2026-11-05T15:00", 30)));
        List<SlotResponse> slots = service.slots(ADMIN, 2L, 7L, LocalDate.parse("2026-11-05"),
                AppointmentDuration.MINUTES_60, null);
        assertThat(reasonAt(slots, "11:30")).isEqualTo(SlotUnavailableReason.PAST);
        assertThat(reasonAt(slots, "12:30")).isEqualTo(SlotUnavailableReason.THERAPIST_BUSY);
        assertThat(reasonAt(slots, "14:30")).isEqualTo(SlotUnavailableReason.PATIENT_BUSY);
        assertThat(reasonAt(slots, "13:00")).isNull();
    }

    @Test
    void theExcludedAppointmentDoesNotBlockItsOwnSlot() {
        when(repository.findOverlappingForTherapistOrPatient(any(), any(), any(), any(), any())).thenReturn(List.of());
        List<SlotResponse> slots = service.slots(ADMIN, 2L, null, LocalDate.parse("2026-11-10"),
                AppointmentDuration.MINUTES_30, 5L);
        verify(repository).findOverlappingForTherapistOrPatient(eq(2L), isNull(), any(), any(), eq(5L));
        assertThat(slots).allMatch(SlotResponse::available);
    }

    private static SlotUnavailableReason reasonAt(List<SlotResponse> slots, String time) {
        return slots.stream().filter(slot -> slot.startTime().equals(LocalTime.parse(time))).findFirst()
                .orElseThrow().reason();
    }

    private static Appointment appointment(User therapist, Patient patient, String start, int minutes) {
        return new Appointment(therapist, patient, null, LocalDateTime.parse(start),
                AppointmentDuration.fromMinutes(minutes).orElseThrow(), null, Instant.parse("2026-11-04T12:00:00Z"));
    }

    private static User user(Long id) {
        User user = User.builder().email("therapist" + id + "@example.com").passwordHash("hash")
                .fullName("Therapist").role(Role.TERAPEUTA).active(true).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static Patient patient(Long id) {
        Patient patient = Patient.builder()
                .dni("3000000" + id)
                .data(new PatientData("Lucía", "Fernández", LocalDate.of(1984, 3, 21), "249 412-3456", "Docente",
                        "OSDE", "61 234567 8 01", Dominance.RIGHT))
                .createdAt(Instant.parse("2026-11-04T12:00:00Z"))
                .build();
        ReflectionTestUtils.setField(patient, "id", id);
        return patient;
    }
}
