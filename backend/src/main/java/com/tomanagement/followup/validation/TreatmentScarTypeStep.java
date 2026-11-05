package com.tomanagement.followup.validation;

import com.tomanagement.catalog.entity.ScarType;
import com.tomanagement.catalog.repository.ScarTypeRepository;
import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.ResourceNotFoundException;

import org.springframework.stereotype.Component;

/**
 * The treatment decides the scar type (H6.1): required and active when it is surgical, rejected when it is
 * conservative.
 */
@Component
public class TreatmentScarTypeStep extends FollowUpValidationStep {

    private final ScarTypeRepository scarTypeRepository;

    public TreatmentScarTypeStep(ScarTypeRepository scarTypeRepository) {
        this.scarTypeRepository = scarTypeRepository;
    }

    @Override
    protected void check(FollowUpValidationContext context) {
        Long scarTypeId = context.request().scarTypeId();
        boolean required = context.diagnosis().getTreatment().requiresScarType();
        if (required && scarTypeId == null) {
            throw new BusinessRuleException(ErrorCode.SCAR_TYPE_REQUIRED);
        }
        if (!required && scarTypeId != null) {
            throw new BusinessRuleException(ErrorCode.SCAR_TYPE_NOT_ALLOWED);
        }
        if (required) {
            ScarType scarType = scarTypeRepository.findById(scarTypeId)
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CATALOG_ITEM_NOT_FOUND));
            if (!scarType.isActive()) {
                throw new BusinessRuleException(ErrorCode.SCAR_TYPE_INACTIVE);
            }
        }
    }
}
