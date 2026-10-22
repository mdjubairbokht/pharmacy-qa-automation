package com.rxcare.qa.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class DashboardPage extends AppPage {

    public DashboardPage(WebDriver driver) {
        super(driver);
    }

    public DashboardPage waitUntilLoaded() {
        waitForReady("view-dashboard");
        return this;
    }

    /** Value of a stat card: NEW, VERIFIED, FILLED, PICKED_UP, patients, low-stock, sales-today. */
    public String stat(String key) {
        return text(By.cssSelector("[data-testid='stat-" + key + "'] .value"));
    }

    public int statAsInt(String key) {
        return Integer.parseInt(stat(key));
    }
}
