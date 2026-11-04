package com.tomanagement.patient.dto;

import java.time.LocalDate;

import com.tomanagement.patient.entity.Dni;
import com.tomanagement.patient.entity.Dominance;
import com.tomanagement.patient.entity.PatientData;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * New patient (H4.1): every field is required. The DNI is normalized to digits before validation. Personal data
 * never appears in {@link #toString()}.
 */
public record CreatePatientRequest(
        @NotBlank(message = "Ingresá el DNI.") @Pattern(regexp = "\\d{6,9}", message = "El DNI tiene que tener entre 6 y 9 números.")
        String dni,
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

    public CreatePatientRequest {
        dni = Dni.normalize(dni);
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
        return "CreatePatientRequest[***]";
    }
}
