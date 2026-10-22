package com.rxcare.qa.api;

import com.rxcare.qa.api.models.Drug;
import io.qameta.allure.Step;
import io.restassured.response.Response;

public class InventoryApi {

    private final String token;

    public InventoryApi(String token) {
        this.token = token;
    }

    @Step("GET /api/drugs")
    public Response drugs() {
        return ApiSpecs.request(token).get("/api/drugs");
    }

    @Step("GET /api/drugs/{ndc}")
    public Response drug(String ndc) {
        return ApiSpecs.request(token).get("/api/drugs/" + ndc);
    }

    public Drug drugOk(String ndc) {
        return drug(ndc).then().statusCode(200).extract().as(Drug.class);
    }

    @Step("GET /api/inventory/low-stock")
    public Response lowStock() {
        return ApiSpecs.request(token).get("/api/inventory/low-stock");
    }

    @Step("POST /api/inventory/{ndc}/adjustments")
    public Response adjust(String ndc, Object body) {
        return ApiSpecs.request(token).body(body).post("/api/inventory/" + ndc + "/adjustments");
    }

    @Step("GET /api/inventory/{ndc}/transactions")
    public Response transactions(String ndc) {
        return ApiSpecs.request(token).get("/api/inventory/" + ndc + "/transactions");
    }
}
