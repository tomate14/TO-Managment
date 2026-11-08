package com.tomanagement.billing.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tomanagement.billing.dto.BilledSessionRef;
import com.tomanagement.billing.dto.BillingResponse;
import com.tomanagement.billing.dto.BillingSummaryResponse;
import com.tomanagement.billing.dto.CreateBillingRequest;
import com.tomanagement.billing.entity.Billing;
import com.tomanagement.billing.entity.BillingFollowUp;
import com.tomanagement.billing.repository.BillingFollowUpRepository;
import com.tomanagement.billing.repository.BillingRepository;
import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.ResourceNotFoundException;
import com.tomanagement.diagnosis.entity.Diagnosis;
import com.tomanagement.diagnosis.repository.DiagnosisRepository;
import com.tomanagement.followup.entity.FollowUp;
import com.tomanagement.followup.repository.FollowUpRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingServiceImpl implements BillingService {

    private static final Comparator<FollowUp> OLDEST_FIRST = Comparator.comparing(FollowUp::getDate)
            .thenComparing(FollowUp::getId);

    private final DiagnosisRepository diagnosisRepository;
    private final FollowUpRepository followUpRepository;
    private final BillingRepository billingRepository;
    private final BillingFollowUpRepository linkRepository;
    private final Clock clock;

    public BillingServiceImpl(DiagnosisRepository diagnosisRepository, FollowUpRepository followUpRepository,
            BillingRepository billingRepository, BillingFollowUpRepository linkRepository, Clock clock) {
        this.diagnosisRepository = diagnosisRepository;
        this.followUpRepository = followUpRepository;
        this.billingRepository = billingRepository;
        this.linkRepository = linkRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public BillingSummaryResponse summary(Long diagnosisId) {
        return summaryOf(findDiagnosis(diagnosisId));
    }

    @Override
    @Transactional
    public BillingSummaryResponse register(Long diagnosisId, CreateBillingRequest request) {
        Diagnosis diagnosis = findDiagnosis(diagnosisId);
        List<Long> requestedIds = request.followUpIdsOrEmpty();
        if (requestedIds.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.BILLING_SESSIONS_REQUIRED);
        }
        if (request.amount().signum() <= 0) {
            throw new BusinessRuleException(ErrorCode.BILLING_AMOUNT_INVALID);
        }
        if (request.billedOn().isAfter(LocalDate.now(clock))) {
            throw new BusinessRuleException(ErrorCode.BILLING_DATE_IN_FUTURE);
        }
        List<FollowUp> sessions = billableSessions(diagnosisId, requestedIds);
        try {
            billingRepository.saveAndFlush(
                    new Billing(diagnosis, request.billedOn(), request.amount(), Instant.now(clock), sessions));
        } catch (DataIntegrityViolationException violation) {
            throw BillingIntegrityTranslator.translate(violation);
        }
        return summaryOf(diagnosis);
    }

    @Override
    @Transactional
    public BillingSummaryResponse voidBilling(Long billingId) {
        Billing billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.BILLING_NOT_FOUND));
        billing.voidBilling(Instant.now(clock));
        return summaryOf(billing.getDiagnosis());
    }

    private Diagnosis findDiagnosis(Long diagnosisId) {
        return diagnosisRepository.findById(diagnosisId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DIAGNOSIS_NOT_FOUND));
    }

    /**
     * The requested follow-ups, in the requested order; each must be of the diagnosis, appear once and not be
     * billed already.
     */
    private List<FollowUp> billableSessions(Long diagnosisId, List<Long> requestedIds) {
        Map<Long, FollowUp> ofDiagnosis = new LinkedHashMap<>();
        followUpRepository.findByDiagnosis_IdOrderByDateDescIdDesc(diagnosisId)
                .forEach(followUp -> ofDiagnosis.put(followUp.getId(), followUp));
        Set<Long> billed = new HashSet<>(linkRepository.findBilledFollowUpIds(diagnosisId));
        Set<Long> distinct = new HashSet<>(requestedIds);
        boolean invalid = distinct.size() != requestedIds.size()
                || !ofDiagnosis.keySet().containsAll(distinct)
                || distinct.stream().anyMatch(billed::contains);
        if (invalid) {
            throw new BusinessRuleException(ErrorCode.BILLING_SESSION_INVALID);
        }
        return requestedIds.stream().map(ofDiagnosis::get).toList();
    }

    private BillingSummaryResponse summaryOf(Diagnosis diagnosis) {
        List<FollowUp> followUps = followUpRepository.findByDiagnosis_IdOrderByDateDescIdDesc(diagnosis.getId())
                .stream().sorted(OLDEST_FIRST).toList();
        Map<Long, BilledSessionRef> refs = new LinkedHashMap<>();
        followUps.forEach(followUp -> refs.put(followUp.getId(), BilledSessionRef.from(followUp)));
        Set<Long> billed = new HashSet<>(linkRepository.findBilledFollowUpIds(diagnosis.getId()));
        List<BilledSessionRef> pending = followUps.stream()
                .filter(followUp -> !billed.contains(followUp.getId()))
                .map(followUp -> refs.get(followUp.getId())).toList();
        List<BillingResponse> billings = billingRepository
                .findByDiagnosis_IdOrderByBilledOnDescIdDesc(diagnosis.getId()).stream()
                .map(billing -> BillingResponse.from(billing, sessionsOf(billing, refs))).toList();
        return new BillingSummaryResponse(followUps.size(), followUps.size() - pending.size(), pending.size(),
                pending, billings);
    }

    /** Sessions of a billing, oldest first (the refs map is already in that order). */
    private static List<BilledSessionRef> sessionsOf(Billing billing, Map<Long, BilledSessionRef> refs) {
        Set<Long> ids = new HashSet<>();
        billing.getLinks().stream().map(BillingFollowUp::getFollowUpId).forEach(ids::add);
        return refs.values().stream().filter(ref -> ids.contains(ref.followUpId())).toList();
    }
}
