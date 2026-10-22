package com.rxcare.pharmacy.prescription;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PharmacyTransaction(int id, int prescriptionId, BigDecimal amount, String paymentMethod,
                                  OffsetDateTime createdAt) {
}
