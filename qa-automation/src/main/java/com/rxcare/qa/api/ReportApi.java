package com.rxcare.qa.api;

import io.qameta.allure.Step;
import io.restassured.response.Response;

import java.time.LocalDate;

public class ReportApi {

    private final String token;

    public ReportApi(String token) {
        this.token = token;
    }

    @Step("POST /api/reports/daily-sales/run?date={date}")
    public Response runDailySales(LocalDate date) {
        return ApiSpecs.request(token).queryParam("date", date.toString()).post("/api/reports/daily-sales/run");
    }

    @Step("GET /api/reports/daily-sales?date={date}")
    public Response dailySales(LocalDate date) {
        return ApiSpecs.request(token).queryParam("date", date.toString()).get("/api/reports/daily-sales");
    }

    @Step("GET /api/dashboard")
    public Response dashboard() {
        return ApiSpecs.request(token).get("/api/dashboard");
    }

    @Step("GET /api/health")
    public Response health() {
        return ApiSpecs.request(null).get("/api/health");
    }
}
