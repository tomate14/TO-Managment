package com.tomanagement.diagnosis.entity;

import java.time.LocalDate;

import com.tomanagement.catalog.entity.SurgeryType;
import com.tomanagement.common.exception.BusinessRuleException;
import com.tomanagement.common.exception.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * Treatment with surgery: surgery date, stitches removal (not before the surgery) and surgery type.
 */
@Entity
@Table(name = "surgical_treatment")
@DiscriminatorValue("SURGICAL")
@PrimaryKeyJoinColumn(name = "treatment_id")
public class SurgicalTreatment extends Treatment {

    @Column(name = "surgery_date", nullable = false, updatable = false)
    private LocalDate surgeryDate;

    @Column(name = "stitches_removal_date", nullable = false, updatable = false)
    private LocalDate stitchesRemovalDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "surgery_type_id", nullable = false, updatable = false)
    private SurgeryType surgeryType;

    protected SurgicalTreatment() {
        // Required by JPA.
    }

    /**
     * @throws BusinessRuleException SURGERY_DATA_REQUIRED or STITCHES_BEFORE_SURGERY
     */
    public SurgicalTreatment(LocalDate surgeryDate, LocalDate stitchesRemovalDate, SurgeryType surgeryType) {
        if (surgeryDate == null || stitchesRemovalDate == null || surgeryType == null) {
            throw new BusinessRuleException(ErrorCode.SURGERY_DATA_REQUIRED);
        }
        if (stitchesRemovalDate.isBefore(surgeryDate)) {
            throw new BusinessRuleException(ErrorCode.STITCHES_BEFORE_SURGERY);
        }
        this.surgeryDate = surgeryDate;
        this.stitchesRemovalDate = stitchesRemovalDate;
        this.surgeryType = surgeryType;
    }

    @Override
    public TreatmentType getType() {
        return TreatmentType.SURGICAL;
    }

    @Override
    public boolean requiresScarType() {
        return true;
    }

    @Override
    public <R> R accept(TreatmentVisitor<R> visitor) {
        return visitor.visitSurgical(this);
    }

    public LocalDate getSurgeryDate() {
        return surgeryDate;
    }

    public LocalDate getStitchesRemovalDate() {
        return stitchesRemovalDate;
    }

    public SurgeryType getSurgeryType() {
        return surgeryType;
    }
}
