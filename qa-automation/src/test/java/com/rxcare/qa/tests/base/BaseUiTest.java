package com.rxcare.qa.tests.base;

import com.rxcare.qa.api.RxApi;
import com.rxcare.qa.config.Users;
import com.rxcare.qa.driver.BrowserManager;
import com.rxcare.qa.listeners.Screenshots;
import com.rxcare.qa.pages.DashboardPage;
import com.rxcare.qa.pages.LoginPage;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * A fresh browser per test method (thread-confined, so classes can run in parallel).
 * Failure evidence is captured here, before the browser is closed.
 */
public abstract class BaseUiTest {

    @BeforeMethod(alwaysRun = true)
    public void startBrowser() {
        BrowserManager.start();
    }

    @AfterMethod(alwaysRun = true)
    public void stopBrowser(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE && BrowserManager.isActive()) {
            Screenshots.captureFailure(BrowserManager.driver(),
                    result.getTestClass().getRealClass().getSimpleName() + "_" + result.getMethod().getMethodName());
        }
        BrowserManager.quit();
    }

    protected WebDriver driver() {
        return BrowserManager.driver();
    }

    protected LoginPage openLogin() {
        return new LoginPage(driver()).open();
    }

    protected DashboardPage loginAsPharmacist() {
        Users.Credentials user = Users.pharmacist();
        return openLogin().loginAs(user.username(), user.password());
    }

    protected DashboardPage loginAsTechnician() {
        Users.Credentials user = Users.technician();
        return openLogin().loginAs(user.username(), user.password());
    }

    /** API access for fast test setup (arrange through the API, assert through the UI). */
    protected RxApi api() {
        return RxApi.asPharmacist();
    }
}
