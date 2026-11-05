package com.tomanagement.appointment.service;

/**
 * The database exclusion constraints that keep appointments from overlapping.
 */
public enum OverlapConstraint {
    THERAPIST("ex_appointment_therapist_overlap"),
    PATIENT("ex_appointment_patient_overlap");

    private final String constraintName;

    OverlapConstraint(String constraintName) {
        this.constraintName = constraintName;
    }

    public String getConstraintName() {
        return constraintName;
    }
}
