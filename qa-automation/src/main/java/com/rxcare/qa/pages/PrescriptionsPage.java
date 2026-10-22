package com.rxcare.qa.pages;

import com.rxcare.qa.pages.components.NewPrescriptionDialog;
import com.rxcare.qa.pages.components.PickupDialog;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/** Prescription work queue: create, verify, fill, pick up, cancel and refill prescriptions. */
public class PrescriptionsPage extends AppPage {

    private static final String TABLE = "rx-table";
    private static final By STATUS_FILTER = testId("rx-status-filter");
    private static final By NEW_RX = testId("new-rx-button");

    public PrescriptionsPage(WebDriver driver) {
        super(driver);
    }

    public PrescriptionsPage waitUntilLoaded() {
        waitForReady(TABLE);
        return this;
    }

    @Step("Filter prescriptions by status {status}")
    public PrescriptionsPage filterByStatus(String status) {
        rerender(TABLE, () -> selectByValue(STATUS_FILTER, status));
        return this;
    }

    @Step("Open the New prescription dialog")
    public NewPrescriptionDialog openNewPrescription() {
        click(NEW_RX);
        return new NewPrescriptionDialog(driver).waitUntilOpen();
    }

    private static By row(String rxNumber) {
        return By.cssSelector("[data-testid='rx-row'][data-rx-number='" + rxNumber + "']");
    }

    private static By action(String rxNumber, String action) {
        return By.cssSelector("[data-testid='rx-row'][data-rx-number='" + rxNumber + "'] "
                + "[data-testid='rx-action-" + action + "']");
    }

    public boolean hasRow(String rxNumber) {
        return !driver.findElements(row(rxNumber)).isEmpty();
    }

    public String statusOf(String rxNumber) {
        return text(By.cssSelector("[data-testid='rx-row'][data-rx-number='" + rxNumber + "'] [data-testid='rx-status']"));
    }

    public String cell(String rxNumber, String column) {
        return text(By.cssSelector("[data-testid='rx-row'][data-rx-number='" + rxNumber + "'] td[data-col='" + column + "']"));
    }

    public PrescriptionsPage waitForStatus(String rxNumber, String expectedStatus) {
        By badge = By.cssSelector("[data-testid='rx-row'][data-rx-number='" + rxNumber + "'] [data-testid='rx-status']");
        wait.until(d -> {
            List<WebElement> found = d.findElements(badge);
            return !found.isEmpty() && expectedStatus.equals(found.get(0).getText().trim());
        });
        return this;
    }

    public boolean isActionAvailable(String rxNumber, String action) {
        return !driver.findElements(action(rxNumber, action)).isEmpty();
    }

    public List<String> statusesShown() {
        return driver.findElements(By.cssSelector("[data-testid='rx-row'] [data-testid='rx-status']"))
                .stream().map(WebElement::getText).map(String::trim).toList();
    }

    @Step("Verify prescription {rxNumber}")
    public PrescriptionsPage verify(String rxNumber) {
        return simpleAction(rxNumber, "verify", "verified", "VERIFIED");
    }

    @Step("Fill prescription {rxNumber}")
    public PrescriptionsPage fill(String rxNumber) {
        return simpleAction(rxNumber, "fill", "filled", "FILLED");
    }

    @Step("Refill prescription {rxNumber}")
    public PrescriptionsPage refill(String rxNumber) {
        return simpleAction(rxNumber, "refill", "refilled", "NEW");
    }

    /** Clicks an action and returns the resulting toast text without assuming success (negative tests). */
    @Step("Click '{action}' on {rxNumber}")
    public String clickActionAndReadToast(String rxNumber, String action) {
        clearToast();
        click(action(rxNumber, action));
        wait.until(d -> isPresentAndVisible(testId("toast")) && !d.findElement(testId("toast")).getText().isBlank());
        return text(testId("toast"));
    }

    @Step("Open pick-up for {rxNumber}")
    public PickupDialog openPickup(String rxNumber) {
        click(action(rxNumber, "pickup"));
        return new PickupDialog(driver).waitUntilOpen();
    }

    @Step("Cancel prescription {rxNumber}: {reason}")
    public PrescriptionsPage cancel(String rxNumber, String reason) {
        click(action(rxNumber, "cancel"));
        visible(testId("cancel-dialog"));
        type(testId("cancel-reason"), reason);
        clearToast();
        click(testId("cancel-confirm"));
        waitForToastContaining(rxNumber + " cancelled");
        return waitForStatus(rxNumber, "CANCELLED");
    }

    private PrescriptionsPage simpleAction(String rxNumber, String action, String toastVerb, String newStatus) {
        clearToast();
        click(action(rxNumber, action));
        waitForToastContaining(rxNumber + " " + toastVerb);
        return waitForStatus(rxNumber, newStatus);
    }
}
