package com.tomanagement.appointment.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.tomanagement.appointment.dto.AppointmentResponse;
import com.tomanagement.appointment.dto.CreateAppointmentRequest;
import com.tomanagement.appointment.dto.RescheduleAppointmentRequest;
import com.tomanagement.appointment.entity.AgendaRange;
import com.tomanagement.appointment.entity.Appointment;
import com.tomanagement.appointment.entity.AppointmentDuration;
import com.tomanagement.appointment.mapper.AppointmentMapper;
import com.tomanagement.appointment.repository.AppointmentRepository;
import com.tomanagement.appointment.repository.AppointmentSpecifications;
import com.tomanagement.appointment.validation.AppointmentDraft;
import com.tomanagement.appointment.validation.AppointmentValidationChain;
import com.tomanagement.appointment.validation.AppointmentValidationContext;
import com.tomanagement.appointment.visibility.AppointmentVisibilityPolicy;
import com.tomanagement.appointment.visibility.AppointmentVisibilityPolicyFactory;
import com.tomanagement.auth.security.AuthenticatedUser;
import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.DuplicateResourceException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.InternalSystemException;
import com.tomanagement.common.exception.ResourceNotFoundException;
import com.tomanagement.diagnosis.entity.Diagnosis;
import com.tomanagement.diagnosis.repository.DiagnosisRepository;
import com.tomanagement.patient.entity.Patient;
import com.tomanagement.patient.repository.PatientRepository;
import com.tomanagement.tenant.CurrentEstablishmentProvider;
import com.tomanagement.user.entity.User;
import com.tomanagement.user.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final AppointmentValidationChain validationChain;
    private final AppointmentVisibilityPolicyFactory policyFactory;
    private final AppointmentMapper mapper;
    private final CurrentEstablishmentProvider currentEstablishment;
    private final Clock clock;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository, UserRepository userRepository,
            PatientRepository patientRepository, DiagnosisRepository diagnosisRepository,
            AppointmentValidationChain validationChain, AppointmentVisibilityPolicyFactory policyFactory,
            AppointmentMapper mapper, CurrentEstablishmentProvider currentEstablishment, Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.diagnosisRepository = diagnosisRepository;
        this.validationChain = validationChain;
        this.policyFactory = policyFactory;
        this.mapper = mapper;
        this.currentEstablishment = currentEstablishment;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> list(AuthenticatedUser actor, LocalDate from, LocalDate to,
            Set<Long> therapistIds) {
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) + 1 > AgendaRange.MAX_DAYS.getDays()) {
            throw new BusinessRuleException(ErrorCode.APPOINTMENT_RANGE_TOO_LONG);
        }
        Set<Long> requested = therapistIds == null ? Set.of() : therapistIds;
        Specification<Appointment> specification = AppointmentSpecifications.startsBetween(from, to);
        Optional<Set<Long>> visible = policy(actor).visibleTherapists(actor, requested);
        if (visible.isPresent()) {
            specification = specification.and(AppointmentSpecifications.ofTherapists(visible.get()));
        }
        return appointmentRepository.findAll(specification, Sort.by("startAt", "id")).stream()
                .map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse get(AuthenticatedUser actor, Long id) {
        Appointment appointment = load(id);
        policy(actor).checkCanManage(actor, appointment.getTherapist().getId());
        return mapper.toResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse book(AuthenticatedUser actor, CreateAppointmentRequest request) {
        AppointmentDuration duration = duration(request.durationMinutes());
        User therapist = findTherapist(request.therapistId());
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PATIENT_NOT_FOUND));
        Diagnosis diagnosis = findDiagnosis(request.diagnosisId());
        LocalDateTime startAt = request.date().atTime(request.startTime());
        validationChain.validate(new AppointmentValidationContext(actor,
                new AppointmentDraft(therapist, patient, diagnosis, startAt, duration, null)));
        Appointment appointment = new Appointment(therapist, patient, diagnosis, startAt, duration,
                blankToNull(request.notes()), Instant.now(clock));
        return mapper.toResponse(saveTranslatingConflicts(appointment));
    }

    @Override
    @Transactional
    public AppointmentResponse reschedule(AuthenticatedUser actor, Long id, RescheduleAppointmentRequest request) {
        Appointment appointment = loadForUpdate(id);
        // The actor must own the CURRENT appointment, so a therapist cannot take someone else's.
        policy(actor).checkCanManage(actor, appointment.getTherapist().getId());
        requireUnconfirmed(appointment);
        AppointmentDuration duration = duration(request.durationMinutes());
        User therapist = findTherapist(request.therapistId());
        Diagnosis diagnosis = findDiagnosis(request.diagnosisId());
        LocalDateTime startAt = request.date().atTime(request.startTime());
        validationChain.validate(new AppointmentValidationContext(actor,
                new AppointmentDraft(therapist, appointment.getPatient(), diagnosis, startAt, duration, id)));
        appointment.reschedule(therapist, diagnosis, startAt, duration, blankToNull(request.notes()));
        return mapper.toResponse(saveTranslatingConflicts(appointment));
    }

    @Override
    @Transactional
    public void cancel(AuthenticatedUser actor, Long id) {
        Appointment appointment = loadForUpdate(id);
        policy(actor).checkCanManage(actor, appointment.getTherapist().getId());
        requireUnconfirmed(appointment);
        appointmentRepository.delete(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse confirm(AuthenticatedUser actor, Long id) {
        Appointment appointment = loadForUpdate(id);
        policy(actor).checkCanManage(actor, appointment.getTherapist().getId());
        appointment.confirm(Instant.now(clock));
        return mapper.toResponse(appointment);
    }

    private void requireUnconfirmed(Appointment appointment) {
        if (appointment.getConfirmedAt() != null) {
            throw new BusinessRuleException(ErrorCode.APPOINTMENT_ALREADY_CONFIRMED);
        }
    }

    private Appointment loadForUpdate(Long id) {
        return appointmentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.APPOINTMENT_NOT_FOUND));
    }

    private AppointmentVisibilityPolicy policy(AuthenticatedUser actor) {
        return policyFactory.forRole(actor.role());
    }

    private Appointment load(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.APPOINTMENT_NOT_FOUND));
    }

    private AppointmentDuration duration(int minutes) {
        return AppointmentDuration.fromMinutes(minutes)
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.APPOINTMENT_DURATION_INVALID));
    }

    /** Users are not tenant-filtered by Hibernate: a therapist of another establishment must look nonexistent. */
    private User findTherapist(Long id) {
        return userRepository.findByIdAndEstablishmentId(id, currentEstablishment.requireEstablishmentId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    private Diagnosis findDiagnosis(Long id) {
        if (id == null) {
            return null;
        }
        return diagnosisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DIAGNOSIS_NOT_FOUND));
    }

    private static String blankToNull(String notes) {
        return notes == null || notes.isBlank() ? null : notes.strip();
    }

    /** Two requests can pass the pre-check together; the database exclusion constraints decide who wins. */
    private Appointment saveTranslatingConflicts(Appointment appointment) {
        try {
            return appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException exception) {
            String message = String.valueOf(exception.getMessage());
            if (message.contains(OverlapConstraint.THERAPIST.getConstraintName())) {
                throw new DuplicateResourceException(ErrorCode.THERAPIST_BUSY);
            }
            if (message.contains(OverlapConstraint.PATIENT.getConstraintName())) {
                throw new DuplicateResourceException(ErrorCode.PATIENT_BUSY);
            }
            throw new InternalSystemException(exception);
        }
    }
}
