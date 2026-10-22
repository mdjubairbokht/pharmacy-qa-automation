package com.rxcare.pharmacy.prescription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PickupRequest(
        @NotBlank @Pattern(regexp = "^(CASH|CARD|INSURANCE)$", message = "must be CASH, CARD or INSURANCE")
        String paymentMethod) {
}
