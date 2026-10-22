package com.rxcare.pharmacy.common;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** Unauthenticated health endpoint used by CI pipelines to wait for the app to be ready. */
@RestController
public class HealthController {

    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("status", "UP");
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            result.put("database", "UP");
        } catch (RuntimeException ex) {
            result.put("database", "DOWN");
        }
        return result;
    }
}
