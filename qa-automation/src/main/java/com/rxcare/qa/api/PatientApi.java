package com.rxcare.qa.api;

import com.rxcare.qa.api.models.Patient;
import io.qameta.allure.Step;
import io.restassured.response.Response;

public class PatientApi {

    private static final String PATH = "/api/patients";
    private final String token;

    public PatientApi(String token) {
        this.token = token;
    }

    @Step("POST /api/patients")
    public Response create(Object body) {
        return ApiSpecs.request(token).body(body).post(PATH);
    }

    public Patient createOk(Object body) {
        return create(body).then().statusCode(201).extract().as(Patient.class);
    }

    @Step("GET /api/patients/{id}")
    public Response get(int id) {
        return ApiSpecs.request(token).get(PATH + "/" + id);
    }

    @Step("GET /api/patients?search={term}")
    public Response search(String term) {
        return ApiSpecs.request(token).queryParam("search", term).get(PATH);
    }

    @Step("PUT /api/patients/{id}")
    public Response update(int id, Object body) {
        return ApiSpecs.request(token).body(body).put(PATH + "/" + id);
    }
}
