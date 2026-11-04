package com.tomanagement.patient.dto;

import java.time.LocalDate;

import com.tomanagement.patient.entity.Dominance;
import com.tomanagement.patient.entity.Patient;

/**
 * A patient as shown in the list and the patient page. The DNI travels as digits; the UI formats it.
 */
public record PatientResponse(
        Long id,
        String dni,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String phone,
        String occupation,
        String healthInsurance,
        String healthInsuranceNumber,
        Dominance dominance) {

    public static PatientResponse from(Patient patient) {
        return new PatientResponse(patient.getId(), patient.getDni(), patient.getFirstName(), patient.getLastName(),
                patient.getBirthDate(), patient.getPhone(), patient.getOccupation(), patient.getHealthInsurance(),
                patient.getHealthInsuranceNumber(), patient.getDominance());
    }

    @Override
    public String toString() {
        return "PatientResponse[id=" + id + ", ***]";
    }
}
