package com.rxcare.qa.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
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
        String verifiedBy,
        String filledBy,
        String pickedUpAt,
        String cancelReason) {
}
