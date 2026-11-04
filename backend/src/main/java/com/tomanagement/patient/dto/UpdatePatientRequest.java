package com.tomanagement.patient.dto;

import java.time.LocalDate;

import com.tomanagement.patient.entity.Dominance;
import com.tomanagement.patient.entity.PatientData;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/**
 * Patient edition (H4.3): same fields as the creation except the DNI, which cannot change.
 */
public record UpdatePatientRequest(
        @NotBlank(message = "Ingresá el nombre.") @Size(max = 80, message = "El nombre es demasiado largo.") String firstName,
        @NotBlank(message = "Ingresá el apellido.") @Size(max = 80, message = "El apellido es demasiado largo.") String lastName,
        @NotNull(message = "Ingresá la fecha de nacimiento.")
        @PastOrPresent(message = "La fecha de nacimiento no puede ser futura.") LocalDate birthDate,
        @NotBlank(message = "Ingresá el teléfono.") @Size(max = 40, message = "El teléfono es demasiado largo.") String phone,
        @NotBlank(message = "Ingresá la ocupación.") @Size(max = 120, message = "La ocupación es demasiado larga.") String occupation,
        @NotBlank(message = "Ingresá la obra social.") @Size(max = 120, message = "La obra social es demasiado larga.")
        String healthInsurance,
        @NotBlank(message = "Ingresá el número de afiliado.") @Size(max = 60, message = "El número de afiliado es demasiado largo.")
        String healthInsuranceNumber,
        @NotNull(message = "Elegí la dominancia.") Dominance dominance) {

    public UpdatePatientRequest {
        firstName = Trim.of(firstName);
        lastName = Trim.of(lastName);
        phone = Trim.of(phone);
        occupation = Trim.of(occupation);
        healthInsurance = Trim.of(healthInsurance);
        healthInsuranceNumber = Trim.of(healthInsuranceNumber);
    }

    public PatientData data() {
        return new PatientData(firstName, lastName, birthDate, phone, occupation, healthInsurance, healthInsuranceNumber,
                dominance);
    }

    @Override
    public String toString() {
        return "UpdatePatientRequest[***]";
    }
}
