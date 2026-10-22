package com.rxcare.pharmacy.patient;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record PatientRequest(
        @NotBlank @Size(max = 50)
        @Pattern(regexp = "^[A-Za-z][A-Za-z '\\-]*$", message = "must contain letters only")
        String firstName,

        @NotBlank @Size(max = 50)
        @Pattern(regexp = "^[A-Za-z][A-Za-z '\\-]*$", message = "must contain letters only")
        String lastName,

        @NotNull @Past
        LocalDate dateOfBirth,

        @NotBlank @Pattern(regexp = "^[MFU]$", message = "must be M, F or U")
        String gender,

        @NotBlank @Pattern(regexp = "^\\d{3}-\\d{3}-\\d{4}$", message = "must match ###-###-####")
        String phone,

        @Email @Size(max = 100)
        String email,

        @Size(max = 200)
        String address,

        @Size(max = 50)
        String insurancePlan,

        List<@NotBlank @Size(max = 50) String> allergies) {
}
