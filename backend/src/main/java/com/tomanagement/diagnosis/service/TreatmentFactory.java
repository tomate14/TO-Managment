package com.tomanagement.diagnosis.service;

import com.tomanagement.catalog.entity.SurgeryType;
import com.tomanagement.catalog.repository.SurgeryTypeRepository;
import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.ResourceNotFoundException;
import com.tomanagement.diagnosis.dto.TreatmentRequest;
import com.tomanagement.diagnosis.entity.ConservativeTreatment;
import com.tomanagement.diagnosis.entity.SurgicalTreatment;
import com.tomanagement.diagnosis.entity.Treatment;

import org.springframework.stereotype.Component;

/**
 * Factory Method: builds the treatment for the requested type. The treatment validates its own data; the factory
 * resolves the surgery type, which must be active for new diagnoses.
 */
@Component
public class TreatmentFactory {

    private final SurgeryTypeRepository surgeryTypeRepository;

    public TreatmentFactory(SurgeryTypeRepository surgeryTypeRepository) {
        this.surgeryTypeRepository = surgeryTypeRepository;
    }

    public Treatment create(TreatmentRequest request) {
        return switch (request.type()) {
            case SURGICAL -> new SurgicalTreatment(request.surgeryDate(), request.stitchesRemovalDate(),
                    activeSurgeryType(request.surgeryTypeId()));
            case CONSERVATIVE -> new ConservativeTreatment(request.immobilizationDate());
        };
    }

    private SurgeryType activeSurgeryType(Long surgeryTypeId) {
        if (surgeryTypeId == null) {
            throw new BusinessRuleException(ErrorCode.SURGERY_DATA_REQUIRED);
        }
        SurgeryType surgeryType = surgeryTypeRepository.findById(surgeryTypeId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CATALOG_ITEM_NOT_FOUND));
        if (!surgeryType.isActive()) {
            throw new BusinessRuleException(ErrorCode.SURGERY_TYPE_INACTIVE);
        }
        return surgeryType;
    }
}
