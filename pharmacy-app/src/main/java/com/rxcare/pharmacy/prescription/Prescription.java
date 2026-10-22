package com.rxcare.pharmacy.prescription;

import java.time.OffsetDateTime;

public record Prescription(
        int id,
        String rxNumber,
        int patientId,
        String patientName,
        String ndc,
        String drugName,
        String prescriber,
        String sig,
        int quantity,
        int daysSupply,
        int refills,
        int refillsRemaining,
        String status,
        int fillCount,
        String createdBy,
        OffsetDateTime createdAt,
        String verifiedBy,
        OffsetDateTime verifiedAt,
        String filledBy,
        OffsetDateTime filledAt,
        OffsetDateTime pickedUpAt,
        String cancelReason) {
}
