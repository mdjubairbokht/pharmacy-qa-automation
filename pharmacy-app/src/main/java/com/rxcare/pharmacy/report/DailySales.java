package com.rxcare.pharmacy.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailySales(LocalDate salesDate, String paymentMethod, int transactionCount, BigDecimal totalAmount) {
}
