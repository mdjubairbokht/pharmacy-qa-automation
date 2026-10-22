package com.rxcare.qa.api;

import com.rxcare.qa.api.models.Prescription;
import io.qameta.allure.Step;
import io.restassured.response.Response;

import java.util.Map;

public class PrescriptionApi {

    private static final String PATH = "/api/prescriptions";
    private final String token;

    public PrescriptionApi(String token) {
        this.token = token;
    }

    @Step("POST /api/prescriptions")
    public Response create(Object body) {
        return ApiSpecs.request(token).body(body).post(PATH);
    }

    public Prescription createOk(Object body) {
        return create(body).then().statusCode(201).extract().as(Prescription.class);
    }

    @Step("GET /api/prescriptions/{id}")
    public Response get(int id) {
        return ApiSpecs.request(token).get(PATH + "/" + id);
    }

    public Prescription getOk(int id) {
        return get(id).then().statusCode(200).extract().as(Prescription.class);
    }

    @Step("GET /api/prescriptions?status={status}")
    public Response listByStatus(String status) {
        return ApiSpecs.request(token).queryParam("status", status).get(PATH);
    }

    @Step("GET /api/prescriptions?patientId={patientId}")
    public Response listByPatient(int patientId) {
        return ApiSpecs.request(token).queryParam("patientId", patientId).get(PATH);
    }

    @Step("POST /api/prescriptions/{id}/verify")
    public Response verify(int id) {
        return ApiSpecs.request(token).post(PATH + "/" + id + "/verify");
    }

    @Step("POST /api/prescriptions/{id}/fill")
    public Response fill(int id) {
        return ApiSpecs.request(token).post(PATH + "/" + id + "/fill");
    }

    @Step("POST /api/prescriptions/{id}/pickup with {paymentMethod}")
    public Response pickup(int id, String paymentMethod) {
        return ApiSpecs.request(token).body(Map.of("paymentMethod", paymentMethod)).post(PATH + "/" + id + "/pickup");
    }

    @Step("POST /api/prescriptions/{id}/cancel")
    public Response cancel(int id, String reason) {
        return ApiSpecs.request(token).body(Map.of("reason", reason)).post(PATH + "/" + id + "/cancel");
    }

    @Step("POST /api/prescriptions/{id}/refill")
    public Response refill(int id) {
        return ApiSpecs.request(token).post(PATH + "/" + id + "/refill");
    }

    @Step("GET /api/prescriptions/{id}/history")
    public Response history(int id) {
        return ApiSpecs.request(token).get(PATH + "/" + id + "/history");
    }

    @Step("GET /api/prescriptions/{id}/transactions")
    public Response transactions(int id) {
        return ApiSpecs.request(token).get(PATH + "/" + id + "/transactions");
    }
}
