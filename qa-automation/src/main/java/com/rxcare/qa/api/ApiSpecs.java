package com.rxcare.qa.api;

import com.rxcare.qa.config.Config;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/** Central REST Assured request specification: base URI, JSON, Allure request/response capture. */
public final class ApiSpecs {

    static {
        // Full request/response is printed to the console only when an assertion fails.
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    private ApiSpecs() {
    }

    public static RequestSpecification base() {
        return new RequestSpecBuilder()
                .setBaseUri(Config.apiBaseUrl())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new AllureRestAssured())
                .build();
    }

    /** Request with an optional bearer token (null = anonymous call). */
    public static RequestSpecification request(String token) {
        RequestSpecification spec = given().spec(base());
        return token == null ? spec : spec.header("Authorization", "Bearer " + token);
    }
}
