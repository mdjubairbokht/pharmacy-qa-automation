package com.rxcare.qa.api;

import com.rxcare.qa.api.models.LoginRequest;
import com.rxcare.qa.config.Users;
import io.qameta.allure.Step;
import io.restassured.response.Response;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthApi {

    private static final Map<String, String> TOKEN_CACHE = new ConcurrentHashMap<>();

    @Step("POST /api/auth/login as {username}")
    public Response login(String username, String password) {
        return ApiSpecs.request(null).body(new LoginRequest(username, password)).post("/api/auth/login");
    }

    @Step("POST /api/auth/login with raw body")
    public Response loginWithBody(String rawBody) {
        return ApiSpecs.request(null).body(rawBody).post("/api/auth/login");
    }

    @Step("GET /api/auth/me")
    public Response me(String token) {
        return ApiSpecs.request(token).get("/api/auth/me");
    }

    @Step("POST /api/auth/logout")
    public Response logout(String token) {
        return ApiSpecs.request(token).post("/api/auth/logout");
    }

    /** Fresh token (new session) for the given account. */
    public String newToken(Users.Credentials credentials) {
        return login(credentials.username(), credentials.password())
                .then().statusCode(200)
                .extract().path("token");
    }

    /** Token reused across tests in the same JVM to keep API suites fast. */
    public static String cachedToken(Users.Credentials credentials) {
        return TOKEN_CACHE.computeIfAbsent(credentials.username(), u -> new AuthApi().newToken(credentials));
    }
}
