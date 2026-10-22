package com.rxcare.qa.tests.ui;

import com.rxcare.qa.config.Config;
import com.rxcare.qa.config.Users;
import com.rxcare.qa.pages.DashboardPage;
import com.rxcare.qa.pages.LoginPage;
import com.rxcare.qa.tests.base.BaseUiTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Epic("Pharmacy Operations")
@Feature("Authentication & Authorization")
public class LoginUiTest extends BaseUiTest {

    @Test(groups = {"ui", "smoke", "regression"}, description = "Pharmacist signs in and lands on the dashboard")
    @Story("RX-101 Secure sign-in")
    @Severity(SeverityLevel.BLOCKER)
    @TmsLink("TC-UI-AUTH-001")
    public void pharmacistSignsIn() {
        DashboardPage dashboard = loginAsPharmacist();

        assertEquals(dashboard.signedInUser(), "Dana Jones, RPh");
        assertEquals(dashboard.signedInRole(), "PHARMACIST");
        assertTrue(dashboard.statAsInt("patients") >= 5, "patient count rendered");
        assertTrue(dashboard.currentUrl().endsWith("#dashboard"), "routed to dashboard");
    }

    @DataProvider(name = "badSignIns")
    public Object[][] badSignIns() {
        return new Object[][]{
                {"wrong password", "rph.jones", "nope", "Invalid username or password"},
                {"unknown user", "ghost.user", "Pharm@123", "Invalid username or password"},
                {"empty form", "", "", "Username and password are required"},
                {"disabled account", Users.disabled().username(), Users.disabled().password(), "Account is disabled"}
        };
    }

    @Test(groups = {"ui", "regression"}, dataProvider = "badSignIns",
            description = "Sign-in errors are shown to the user and no session is created")
    @Story("RX-101 Secure sign-in")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("TC-UI-AUTH-002")
    public void signInErrorsAreShown(String scenario, String username, String password, String expectedMessage) {
        LoginPage login = openLogin().loginExpectingError(username, password);

        assertTrue(login.errorMessage().contains(expectedMessage),
                scenario + ": expected '" + expectedMessage + "' but was '" + login.errorMessage() + "'");
        assertTrue(login.currentUrl().contains("index.html"), "still on the login page");
    }

    @Test(groups = {"ui", "regression"}, description = "Signing out ends the session; the app is no longer reachable")
    @Story("RX-101 Secure sign-in")
    @TmsLink("TC-UI-AUTH-003")
    public void signOutEndsSession() {
        LoginPage login = loginAsTechnician().logout();
        assertTrue(login.isDisplayed(), "login page shown after sign-out");

        driver().get(Config.uiBaseUrl() + "/app.html#prescriptions");
        assertTrue(new LoginPage(driver()).waitUntilLoaded().isDisplayed(), "redirected back to sign-in");
    }

    @Test(groups = {"ui", "regression"}, description = "Opening the app without signing in redirects to the login page")
    @Story("RX-101 Secure sign-in")
    @TmsLink("TC-UI-AUTH-004")
    public void deepLinkRequiresSignIn() {
        driver().get(Config.uiBaseUrl() + "/app.html#inventory");
        assertTrue(new LoginPage(driver()).waitUntilLoaded().isDisplayed());
    }
}
