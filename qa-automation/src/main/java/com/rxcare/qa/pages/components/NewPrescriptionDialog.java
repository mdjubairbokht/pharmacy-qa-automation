package com.rxcare.qa.pages.components;

import com.rxcare.qa.api.models.PrescriptionRequest;
import com.rxcare.qa.pages.BasePage;
import com.rxcare.qa.pages.PrescriptionsPage;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewPrescriptionDialog extends BasePage {

    private static final By DIALOG = testId("rx-dialog");
    private static final By ERROR = testId("rx-form-error");
    private static final Pattern RX_NUMBER = Pattern.compile("(RX\\d+)");

    public NewPrescriptionDialog(WebDriver driver) {
        super(driver);
    }

    public NewPrescriptionDialog waitUntilOpen() {
        visible(DIALOG);
        // Patient and drug lists are loaded asynchronously when the dialog opens.
        wait.until(d -> d.findElements(By.cssSelector("[data-testid='rx-drug'] option")).size() > 1);
        return this;
    }

    @Step("Fill prescription form")
    public NewPrescriptionDialog fill(PrescriptionRequest rx) {
        selectByValue(testId("rx-patient"), String.valueOf(rx.patientId()));
        selectByValue(testId("rx-drug"), rx.ndc());
        type(testId("rx-quantity"), rx.quantity() == null ? "" : rx.quantity().toString());
        type(testId("rx-days-supply"), rx.daysSupply() == null ? "" : rx.daysSupply().toString());
        type(testId("rx-refills"), rx.refills() == null ? "" : rx.refills().toString());
        type(testId("rx-prescriber"), rx.prescriber());
        type(testId("rx-sig"), rx.sig());
        return this;
    }

    /** Submits the form and returns the generated Rx number announced in the confirmation toast. */
    @Step("Create prescription")
    public String create() {
        clearToast();
        click(testId("rx-save"));
        String toast = waitForToastContaining("created");
        waitUntilHidden(DIALOG);
        Matcher matcher = RX_NUMBER.matcher(toast);
        if (!matcher.find()) {
            throw new AssertionError("No Rx number in confirmation: " + toast);
        }
        String rxNumber = matcher.group(1);
        new PrescriptionsPage(driver).waitForStatus(rxNumber, "NEW");
        return rxNumber;
    }

    @Step("Create prescription (expecting an error)")
    public NewPrescriptionDialog createExpectingError() {
        click(testId("rx-save"));
        visible(ERROR);
        return this;
    }

    public String errorMessage() {
        return text(ERROR);
    }
}
