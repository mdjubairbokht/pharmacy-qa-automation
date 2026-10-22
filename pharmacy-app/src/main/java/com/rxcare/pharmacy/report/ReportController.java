package com.rxcare.pharmacy.report;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/reports/daily-sales")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @PostMapping("/run")
    public List<DailySales> run(@RequestParam(name = "date", required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.runDailySales(date != null ? date : LocalDate.now(ZoneOffset.UTC));
    }

    @GetMapping
    public List<DailySales> get(@RequestParam(name = "date", required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.dailySales(date != null ? date : LocalDate.now(ZoneOffset.UTC));
    }
}
