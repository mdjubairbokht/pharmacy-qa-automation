package com.rxcare.pharmacy.report;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * A small ETL job: extracts point-of-sale rows from pharmacy_transactions,
 * aggregates them per (UTC) day and payment method and loads the result into
 * daily_sales_summary. Re-running for the same date is idempotent.
 */
@Service
public class ReportService {

    private final JdbcTemplate jdbc;

    public ReportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public List<DailySales> runDailySales(LocalDate date) {
        jdbc.update("DELETE FROM daily_sales_summary WHERE sales_date = ?", date);
        jdbc.update("INSERT INTO daily_sales_summary (sales_date, payment_method, transaction_count, total_amount) "
                + "SELECT (created_at AT TIME ZONE 'UTC')::date, payment_method, COUNT(*), SUM(amount) "
                + "FROM pharmacy_transactions "
                + "WHERE (created_at AT TIME ZONE 'UTC')::date = ? "
                + "GROUP BY (created_at AT TIME ZONE 'UTC')::date, payment_method", date);
        return dailySales(date);
    }

    public List<DailySales> dailySales(LocalDate date) {
        return jdbc.query("SELECT * FROM daily_sales_summary WHERE sales_date = ? ORDER BY payment_method",
                (rs, n) -> new DailySales(
                        rs.getObject("sales_date", LocalDate.class),
                        rs.getString("payment_method"),
                        rs.getInt("transaction_count"),
                        rs.getBigDecimal("total_amount")),
                date);
    }
}
