package com.rxcare.pharmacy.drug;

import java.time.OffsetDateTime;

public record InventoryTransaction(
        int id,
        String ndc,
        Integer prescriptionId,
        String txnType,
        int quantity,
        String reason,
        String performedBy,
        OffsetDateTime createdAt) {
}
