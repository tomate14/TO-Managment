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
 * Treatment without surgery: only the immobilization date.
 */
@Entity
@Table(name = "conservative_treatment")
@DiscriminatorValue("CONSERVATIVE")
@PrimaryKeyJoinColumn(name = "treatment_id")
public class ConservativeTreatment extends Treatment {

    @Column(name = "immobilization_date", nullable = false, updatable = false)
    private LocalDate immobilizationDate;

    protected ConservativeTreatment() {
        // Required by JPA.
    }

    /**
     * @throws BusinessRuleException IMMOBILIZATION_DATE_REQUIRED
     */
    public ConservativeTreatment(LocalDate immobilizationDate) {
        if (immobilizationDate == null) {
            throw new BusinessRuleException(ErrorCode.IMMOBILIZATION_DATE_REQUIRED);
        }
        this.immobilizationDate = immobilizationDate;
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

    public LocalDate getImmobilizationDate() {
        return immobilizationDate;
    }
}
