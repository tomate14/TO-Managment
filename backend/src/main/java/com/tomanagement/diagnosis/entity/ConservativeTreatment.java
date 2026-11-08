package com.tomanagement.diagnosis.entity;

import java.time.LocalDate;

import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * Treatment without surgery: whether it has immobilization and, if so, the immobilization date.
 */
@Entity
@Table(name = "conservative_treatment")
@DiscriminatorValue("CONSERVATIVE")
@PrimaryKeyJoinColumn(name = "treatment_id")
public class ConservativeTreatment extends Treatment {

    @Column(name = "has_immobilization", nullable = false, updatable = false)
    private boolean hasImmobilization;

    @Column(name = "immobilization_date", updatable = false)
    private LocalDate immobilizationDate;

    protected ConservativeTreatment() {
        // Required by JPA.
    }

    /**
     * @throws BusinessRuleException IMMOBILIZATION_ANSWER_REQUIRED when the yes/no answer is missing,
     *         IMMOBILIZATION_DATE_REQUIRED when it has immobilization but no date
     */
    public ConservativeTreatment(Boolean hasImmobilization, LocalDate immobilizationDate) {
        if (hasImmobilization == null) {
            throw new BusinessRuleException(ErrorCode.IMMOBILIZATION_ANSWER_REQUIRED);
        }
        if (hasImmobilization && immobilizationDate == null) {
            throw new BusinessRuleException(ErrorCode.IMMOBILIZATION_DATE_REQUIRED);
        }
        this.hasImmobilization = hasImmobilization;
        this.immobilizationDate = hasImmobilization ? immobilizationDate : null;
    }

    @Override
    public TreatmentType getType() {
        return TreatmentType.CONSERVATIVE;
    }

    @Override
    public boolean requiresScarType() {
        return false;
    }

    @Override
    public <R> R accept(TreatmentVisitor<R> visitor) {
        return visitor.visitConservative(this);
    }

    public boolean hasImmobilization() {
        return hasImmobilization;
    }

    /** Null when the treatment has no immobilization. */
    public LocalDate getImmobilizationDate() {
        return immobilizationDate;
    }
}
