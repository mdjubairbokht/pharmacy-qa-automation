package com.rxcare.qa.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

/** Header + side navigation shared by every screen after sign-in. */
public abstract class AppPage extends BasePage {

    protected AppPage(WebDriver driver) {
        super(driver);
    }

    public String signedInUser() {
        return text(testId("current-user"));
    }

    public String signedInRole() {
        return text(testId("current-role"));
    }

    @Step("Sign out")
    public LoginPage logout() {
        click(testId("logout-button"));
        return new LoginPage(driver).waitUntilLoaded();
    }

    @Step("Open Dashboard")
    public DashboardPage goToDashboard() {
        markStale("view-dashboard");
        click(testId("nav-dashboard"));
        return new DashboardPage(driver).waitUntilLoaded();
    }

    @Step("Open Patients")
    public PatientsPage goToPatients() {
        markStale("patients-table");
        click(testId("nav-patients"));
        return new PatientsPage(driver).waitUntilLoaded();
    }

    @Step("Open Prescriptions")
    public PrescriptionsPage goToPrescriptions() {
        markStale("rx-table");
        click(testId("nav-prescriptions"));
        return new PrescriptionsPage(driver).waitUntilLoaded();
    }

    @Step("Open Inventory")
    public InventoryPage goToInventory() {
        markStale("inventory-table");
        click(testId("nav-inventory"));
        return new InventoryPage(driver).waitUntilLoaded();
    }
}
