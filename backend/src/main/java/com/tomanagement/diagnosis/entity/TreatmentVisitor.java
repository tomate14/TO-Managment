package com.tomanagement.diagnosis.entity;

/**
 * Visitor over the treatment types, so mapping code reads subtype data without instanceof or casts.
 */
public interface TreatmentVisitor<R> {

    R visitSurgical(SurgicalTreatment treatment);

    R visitConservative(ConservativeTreatment treatment);
}
