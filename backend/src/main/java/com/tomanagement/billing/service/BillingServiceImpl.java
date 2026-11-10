package com.tomanagement.billing.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tomanagement.appointment.entity.Appointment;
import com.tomanagement.appointment.repository.AppointmentRepository;
import com.tomanagement.billing.dto.BilledSessionRef;
import com.tomanagement.billing.dto.BillingResponse;
import com.tomanagement.billing.dto.BillingSummaryResponse;
import com.tomanagement.billing.dto.CreateBillingRequest;
import com.tomanagement.billing.entity.Billing;
import com.tomanagement.billing.entity.BillingSessionOrigin;
import com.tomanagement.billing.repository.BillingAppointmentRepository;
import com.tomanagement.billing.repository.BillingRepository;
import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.ResourceNotFoundException;
import com.tomanagement.followup.entity.FollowUp;
import com.tomanagement.patient.entity.Patient;
import com.tomanagement.patient.repository.PatientRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingServiceImpl implements BillingService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final BillingRepository billingRepository;
    private final BillingAppointmentRepository linkRepository;
    private final Clock clock;

    public BillingServiceImpl(PatientRepository patientRepository, AppointmentRepository appointmentRepository,
            BillingRepository billingRepository, BillingAppointmentRepository linkRepository, Clock clock) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.billingRepository = billingRepository;
        this.linkRepository = linkRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public BillingSummaryResponse summary(Long patientId) {
        return summaryOf(findPatient(patientId));
    }

    @Override
    @Transactional
    public BillingSummaryResponse register(Long patientId, CreateBillingRequest request) {
        Patient patient = findPatient(patientId);
        List<Long> requestedIds = request.appointmentIdsOrEmpty();
        if (requestedIds.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.BILLING_SESSIONS_REQUIRED);
        }
        if (request.amount().signum() <= 0) {
            throw new BusinessRuleException(ErrorCode.BILLING_AMOUNT_INVALID);
        }
        if (request.billedOn().isAfter(LocalDate.now(clock))) {
            throw new BusinessRuleException(ErrorCode.BILLING_DATE_IN_FUTURE);
        }
        List<Appointment> sessions = billableSessions(patientId, requestedIds);
        try {
            billingRepository.saveAndFlush(
                    new Billing(patient, request.billedOn(), request.amount(), Instant.now(clock), sessions));
        } catch (DataIntegrityViolationException violation) {
            throw BillingIntegrityTranslator.translate(violation);
        }
        return summaryOf(patient);
    }

    @Override
    @Transactional
    public BillingSummaryResponse voidBilling(Long billingId) {
        Billing billing = billingRepository.findByIdForUpdate(billingId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.BILLING_NOT_FOUND));
        // Use the same order as registration, confirmation, cancellation and rescheduling.
        billing.getAppointments().stream().map(Appointment::getId).sorted()
                .forEach(id -> appointmentRepository.findByIdForUpdate(id)
                        .orElseThrow(() -> new BusinessRuleException(ErrorCode.BILLING_SESSION_INVALID)));
        billing.voidBilling(Instant.now(clock));
        billingRepository.flush();
        return summaryOf(billing.getPatient());
    }

    private Patient findPatient(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PATIENT_NOT_FOUND));
    }

    private List<Appointment> billableSessions(Long patientId, List<Long> requestedIds) {
        Set<Long> distinct = new HashSet<>(requestedIds);
        if (distinct.contains(null) || distinct.size() != requestedIds.size()) {
            throw new BusinessRuleException(ErrorCode.BILLING_SESSION_INVALID);
        }
        List<Appointment> sessions = new ArrayList<>();
        // Acquire every lock before checking eligibility. A waiting transaction then sees committed links.
        requestedIds.stream().sorted().forEach(id -> sessions.add(appointmentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.BILLING_SESSION_INVALID))));
        boolean invalid = sessions.stream().anyMatch(appointment -> !patientId.equals(appointment.getPatient().getId())
                || appointment.getConfirmedAt() == null);
        if (invalid || !linkRepository.findBilledAmong(requestedIds).isEmpty()) {
            throw new BusinessRuleException(ErrorCode.BILLING_SESSION_INVALID);
        }
        return sessions;
    }

    private BillingSummaryResponse summaryOf(Patient patient) {
        List<Appointment> appointments = appointmentRepository
                .findByPatientIdAndConfirmedAtIsNotNullOrderByStartAtAscIdAsc(patient.getId());
        Set<Long> billed = appointments.isEmpty() ? Set.of() : new HashSet<>(linkRepository
                .findBilledAmong(appointments.stream().map(Appointment::getId).toList()));
        List<BilledSessionRef> pending = appointments.stream()
                .filter(appointment -> !billed.contains(appointment.getId())).map(BilledSessionRef::from).toList();
        List<BillingResponse> billings = billingRepository
                .findByPatient_IdOrderByBilledOnDescIdDesc(patient.getId()).stream()
                .map(billing -> BillingResponse.from(billing, sessionsOf(billing))).toList();
        return new BillingSummaryResponse(appointments.size(), appointments.size() - pending.size(), pending.size(),
                pending, billings);
    }

    private static List<BilledSessionRef> sessionsOf(Billing billing) {
        if (billing.getOrigin() == BillingSessionOrigin.LEGACY_FOLLOW_UP) {
            return billing.getFollowUps().stream().sorted(Comparator.comparing(FollowUp::getDate)
                    .thenComparing(FollowUp::getId)).map(BilledSessionRef::from).toList();
        }
        return billing.getAppointments().stream().sorted(Comparator.comparing(Appointment::getStartAt)
                .thenComparing(Appointment::getId)).map(BilledSessionRef::from).toList();
    }
}
