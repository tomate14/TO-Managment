package com.tomanagement.diagnosis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import com.tomanagement.catalog.entity.SurgeryType;
import com.tomanagement.catalog.repository.SurgeryTypeRepository;
import com.tomanagement.common.exception.ApplicationException;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.common.exception.ResourceNotFoundException;
import com.tomanagement.diagnosis.dto.TreatmentRequest;
import com.tomanagement.diagnosis.entity.ConservativeTreatment;
import com.tomanagement.diagnosis.entity.SurgicalTreatment;
import com.tomanagement.diagnosis.entity.Treatment;
import com.tomanagement.diagnosis.entity.TreatmentType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TreatmentFactoryTest {

    private static final LocalDate SURGERY = LocalDate.of(2026, 10, 14);
    private static final LocalDate STITCHES = LocalDate.of(2026, 10, 28);

    private SurgeryTypeRepository surgeryTypes;
    private TreatmentFactory factory;
    private final SurgeryType plate = new SurgeryType("Osteosíntesis con placa");

    @BeforeEach
    void setUp() {
        surgeryTypes = mock(SurgeryTypeRepository.class);
        when(surgeryTypes.findById(1L)).thenReturn(Optional.of(plate));
        factory = new TreatmentFactory(surgeryTypes);
    }

    @Test
    void buildsASurgicalTreatmentThatRequiresAScarType() {
        Treatment treatment = factory.create(new TreatmentRequest(TreatmentType.SURGICAL, SURGERY, STITCHES, 1L, null));

        assertThat(treatment).isInstanceOf(SurgicalTreatment.class);
        assertThat(treatment.getType()).isEqualTo(TreatmentType.SURGICAL);
        assertThat(treatment.requiresScarType()).isTrue();
        assertThat(((SurgicalTreatment) treatment).getSurgeryType()).isSameAs(plate);
    }

    @Test
    void buildsAConservativeTreatmentWithoutScarType() {
        Treatment treatment = factory.create(
                new TreatmentRequest(TreatmentType.CONSERVATIVE, SURGERY, null, 1L, LocalDate.of(2026, 8, 2)));

        assertThat(treatment).isInstanceOf(ConservativeTreatment.class);
        assertThat(treatment.requiresScarType()).isFalse();
    }

    @Test
    void stitchesRemovalCanBeTheSameDayButNotBeforeTheSurgery() {
        assertThat(factory.create(new TreatmentRequest(TreatmentType.SURGICAL, SURGERY, SURGERY, 1L, null))).isNotNull();
        assertThatThrownBy(() -> factory.create(
                new TreatmentRequest(TreatmentType.SURGICAL, SURGERY, SURGERY.minusDays(1), 1L, null)))
                .extracting(error -> ((ApplicationException) error).getErrorCode())
                .isEqualTo(ErrorCode.STITCHES_BEFORE_SURGERY);
    }

    @Test
    void surgicalDataIsRequired() {
        assertThatThrownBy(() -> factory.create(new TreatmentRequest(TreatmentType.SURGICAL, null, STITCHES, 1L, null)))
                .extracting(error -> ((ApplicationException) error).getErrorCode())
                .isEqualTo(ErrorCode.SURGERY_DATA_REQUIRED);
        assertThatThrownBy(() -> factory.create(new TreatmentRequest(TreatmentType.SURGICAL, SURGERY, STITCHES, null, null)))
                .extracting(error -> ((ApplicationException) error).getErrorCode())
                .isEqualTo(ErrorCode.SURGERY_DATA_REQUIRED);
    }

    @Test
    void conservativeNeedsTheImmobilizationDate() {
        assertThatThrownBy(() -> factory.create(new TreatmentRequest(TreatmentType.CONSERVATIVE, null, null, null, null)))
                .extracting(error -> ((ApplicationException) error).getErrorCode())
                .isEqualTo(ErrorCode.IMMOBILIZATION_DATE_REQUIRED);
    }

    @Test
    void theSurgeryTypeMustExistAndBeActive() {
        SurgeryType retired = new SurgeryType("Artrodesis");
        retired.changeActive(false);
        when(surgeryTypes.findById(2L)).thenReturn(Optional.of(retired));
        when(surgeryTypes.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> factory.create(new TreatmentRequest(TreatmentType.SURGICAL, SURGERY, STITCHES, 2L, null)))
                .extracting(error -> ((ApplicationException) error).getErrorCode())
                .isEqualTo(ErrorCode.SURGERY_TYPE_INACTIVE);
        assertThatThrownBy(() -> factory.create(new TreatmentRequest(TreatmentType.SURGICAL, SURGERY, STITCHES, 3L, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
