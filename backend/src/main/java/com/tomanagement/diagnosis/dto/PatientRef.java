package com.tomanagement.diagnosis.dto;

import com.tomanagement.patient.entity.Patient;

/**
 * Minimal patient data for the diagnosis screens' header ("Nombre Apellido · DNI").
 */
public record PatientRef(Long id, String firstName, String lastName, String dni) {

    public static PatientRef from(Patient patient) {
        return new PatientRef(patient.getId(), patient.getFirstName(), patient.getLastName(), patient.getDni());
    }

    @Override
    public String toString() {
        return "PatientRef[id=" + id + ", ***]";
    }
}
