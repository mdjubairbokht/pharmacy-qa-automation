package com.rxcare.pharmacy.drug;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * RECEIVE = stock delivered from the wholesaler (must be positive).
 * ADJUST  = cycle-count correction, damaged or expired stock (positive or negative).
 */
public record InventoryAdjustmentRequest(
        @NotNull Integer quantityChange,
        @NotBlank @Pattern(regexp = "^(RECEIVE|ADJUST)$", message = "must be RECEIVE or ADJUST") String type,
        @Size(max = 200) String reason) {
}
