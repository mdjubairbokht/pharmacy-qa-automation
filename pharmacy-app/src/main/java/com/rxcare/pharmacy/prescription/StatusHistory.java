package com.rxcare.pharmacy.prescription;

import java.time.OffsetDateTime;

public record StatusHistory(String fromStatus, String toStatus, String changedBy, OffsetDateTime changedAt) {
}
