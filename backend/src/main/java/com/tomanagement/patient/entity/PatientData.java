package com.tomanagement.patient.entity;

import java.time.LocalDate;

/**
 * The editable personal data of a patient (everything but the DNI), already validated and trimmed.
 */
public record PatientData(
        String firstName,
        String lastName,
        LocalDate birthDate,
        String phone,
        String occupation,
        String healthInsurance,
        String healthInsuranceNumber,
        Dominance dominance) {
}
