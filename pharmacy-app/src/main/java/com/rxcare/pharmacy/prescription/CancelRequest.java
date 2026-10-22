package com.rxcare.pharmacy.prescription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelRequest(@NotBlank @Size(max = 200) String reason) {
}
