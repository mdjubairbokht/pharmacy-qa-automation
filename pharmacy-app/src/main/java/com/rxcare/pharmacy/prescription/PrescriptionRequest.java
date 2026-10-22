package com.rxcare.pharmacy.prescription;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PrescriptionRequest(
        @NotNull @Positive Integer patientId,

        @NotBlank @Pattern(regexp = "^\\d{5}-\\d{4}-\\d{2}$", message = "must be an NDC in 5-4-2 format")
        String ndc,

        @NotBlank @Size(max = 100) String prescriber,

        @NotBlank @Size(max = 200) String sig,

        @NotNull @Positive Integer quantity,

        @NotNull @Min(1) @Max(90) Integer daysSupply,

        @NotNull @Min(0) @Max(11) Integer refills) {
}
