package com.rxcare.qa.pages.components;

import com.rxcare.qa.pages.BasePage;
import com.rxcare.qa.pages.PrescriptionsPage;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.math.BigDecimal;

/** Point-of-sale dialog shown when a filled prescription is handed to the patient. */
public class PickupDialog extends BasePage {

    private static final By DIALOG = testId("pickup-dialog");

    public PickupDialog(WebDriver driver) {
        super(driver);
    }

    public PickupDialog waitUntilOpen() {
        visible(DIALOG);
        return this;
    }

    public String rxNumber() {
        return text(testId("pickup-rx-number"));
    }

    /** Amount due as shown to the cashier, e.g. "$13.50" -> 13.50. */
    public BigDecimal amountDue() {
        return new BigDecimal(text(testId("pickup-amount")).replace("$", "").replace(",", ""));
    }

    @Step("Complete sale paid by {paymentMethod}")
    public PrescriptionsPage completeSale(String paymentMethod) {
        String rx = rxNumber();
        selectByValue(testId("pickup-payment"), paymentMethod);
        clearToast();
        click(testId("pickup-confirm"));
        waitForToastContaining(rx + " picked up");
        waitUntilHidden(DIALOG);
        return new PrescriptionsPage(driver).waitForStatus(rx, "PICKED_UP");
    }
}
