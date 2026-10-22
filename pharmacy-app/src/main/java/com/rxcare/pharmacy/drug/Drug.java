package com.rxcare.pharmacy.drug;

import java.math.BigDecimal;

public record Drug(
        String ndc,
        String name,
        String strength,
        String dosageForm,
        String drugClass,
        int schedule,
        BigDecimal unitPrice,
        int qtyOnHand,
        int reorderLevel,
        boolean lowStock) {
}
