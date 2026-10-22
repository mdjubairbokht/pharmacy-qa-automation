package com.rxcare.qa.tests.api;

import com.rxcare.qa.config.Users;
import com.rxcare.qa.data.TestDataReader;
import com.rxcare.qa.tests.base.BaseApiTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@Epic("Pharmacy Operations")
@Feature("Authentication & Authorization")
public class AuthApiTest extends BaseApiTest {

    @Test(groups = {"api", "smoke", "regression"},
            description = "Pharmacist signs in and receives a bearer token and role")
    @Story("RX-101 Secure sign-in")
    @Severity(SeverityLevel.BLOCKER)
    @TmsLink("TC-AUTH-001")
    public void pharmacistCanSignIn() {
        Users.Credentials user = Users.pharmacist();
        auth.login(user.username(), user.password())
                .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/login-response.json"))
                .body("username", equalTo(user.username()))
                .body("role", equalTo("PHARMACIST"))
                .body("token", not(emptyOrNullString()))
                .time(lessThan(3000L));
    }

    @DataProvider(name = "invalidLogins")
    public Object[][] invalidLogins() {
        return TestDataReader.csv("testdata/invalid-logins.csv");
    }

    @Test(groups = {"api", "regression"}, dataProvider = "invalidLogins",
            description = "Invalid, blank, disabled and injection-style credentials are rejected without a token")
    @Story("RX-101 Secure sign-in")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TC-AUTH-002")
    public void invalidCredentialsAreRejected(String scenario, String username, String password,
                                              String expectedStatus, String expectedCode) {
        auth.login(username, password)
                .then()
                .statusCode(Integer.parseInt(expectedStatus))
                .body(matchesJsonSchemaInClasspath("schemas/error.json"))
                .body("code", equalTo(expectedCode))
                .body("token", nullValue());
    }

    @Test(groups = {"api", "regression"}, description = "Malformed JSON body returns 400 MALFORMED_REQUEST")
    @Story("RX-101 Secure sign-in")
    @TmsLink("TC-AUTH-003")
    public void malformedBodyIsRejected() {
        auth.loginWithBody("{\"username\": \"rph.jones\", ")
                .then()
                .statusCode(400)
                .body("code", equalTo("MALFORMED_REQUEST"));
    }

    @Test(groups = {"api", "smoke", "regression"}, description = "Protected endpoints require a valid token")
    @Story("RX-101 Secure sign-in")
    @Severity(SeverityLevel.BLOCKER)
    @TmsLink("TC-AUTH-004")
    public void protectedEndpointsRequireToken() {
        auth.me(null).then().statusCode(401).body("code", equalTo("UNAUTHORIZED"));
        auth.me("not-a-real-token").then().statusCode(401).body("code", equalTo("UNAUTHORIZED"));
    }

    @Test(groups = {"api", "regression"}, description = "Logout invalidates the session token")
    @Story("RX-101 Secure sign-in")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TC-AUTH-005")
    public void logoutInvalidatesToken() {
        String token = auth.newToken(Users.technician());
        auth.me(token).then().statusCode(200).body("role", equalTo("TECHNICIAN"));

        auth.logout(token).then().statusCode(204);

        auth.me(token).then().statusCode(401);
    }
}
