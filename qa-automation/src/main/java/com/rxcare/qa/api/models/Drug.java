package com.rxcare.qa.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
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
