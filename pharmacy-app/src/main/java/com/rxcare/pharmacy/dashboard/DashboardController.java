package com.rxcare.pharmacy.dashboard;

import com.rxcare.pharmacy.prescription.PrescriptionStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class DashboardController {

    private final JdbcTemplate jdbc;

    public DashboardController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record DashboardSummary(Map<String, Integer> prescriptionsByStatus,
                                   int patientCount,
                                   int lowStockCount,
                                   BigDecimal salesToday) {
    }

    @GetMapping("/api/dashboard")
    public DashboardSummary summary() {
        Map<String, Integer> byStatus = new LinkedHashMap<>();
        for (PrescriptionStatus status : PrescriptionStatus.values()) {
            byStatus.put(status.name(), 0);
        }
        jdbc.query("SELECT status, COUNT(*) AS cnt FROM prescriptions GROUP BY status",
                rs -> {
                    byStatus.put(rs.getString("status"), rs.getInt("cnt"));
                });

        Integer patients = jdbc.queryForObject("SELECT COUNT(*) FROM patients", Integer.class);
        Integer lowStock = jdbc.queryForObject(
                "SELECT COUNT(*) FROM drugs WHERE qty_on_hand <= reorder_level", Integer.class);
        BigDecimal sales = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM pharmacy_transactions "
                        + "WHERE (created_at AT TIME ZONE 'UTC')::date = (NOW() AT TIME ZONE 'UTC')::date",
                BigDecimal.class);
        return new DashboardSummary(byStatus, patients == null ? 0 : patients,
                lowStock == null ? 0 : lowStock, sales);
    }
}
