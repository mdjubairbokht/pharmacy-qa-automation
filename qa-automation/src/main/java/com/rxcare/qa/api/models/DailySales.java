package com.rxcare.qa.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DailySales(String salesDate, String paymentMethod, int transactionCount, BigDecimal totalAmount) {
}
