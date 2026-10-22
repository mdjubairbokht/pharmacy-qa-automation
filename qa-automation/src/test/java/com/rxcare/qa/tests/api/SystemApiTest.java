package com.rxcare.qa.tests.api;

import com.rxcare.qa.api.RxApi;
import com.rxcare.qa.tests.base.BaseApiTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@Epic("Platform")
@Feature("Health & Dashboard")
public class SystemApiTest extends BaseApiTest {

    @Test(groups = {"api", "smoke", "regression"}, description = "Health endpoint reports application and database UP")
    @Story("Release readiness")
    @Severity(SeverityLevel.BLOCKER)
    @TmsLink("TC-SYS-001")
    public void healthCheck() {
        RxApi.anonymous().reports().health()
                .then().statusCode(200)
                .body("status", equalTo("UP"))
                .body("database", equalTo("UP"));
    }

    @Test(groups = {"api", "smoke", "regression"}, description = "Dashboard summary exposes counts for every status")
    @Story("RX-104 Prescription workflow")
    @TmsLink("TC-SYS-002")
    public void dashboardSummary() {
        technician.reports().dashboard()
                .then().statusCode(200)
                .body("prescriptionsByStatus.NEW", greaterThanOrEqualTo(0))
                .body("prescriptionsByStatus.CANCELLED", notNullValue())
                .body("patientCount", greaterThanOrEqualTo(5))
                .body("lowStockCount", greaterThanOrEqualTo(1));

        RxApi.anonymous().reports().dashboard().then().statusCode(401);
    }
}
