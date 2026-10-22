package com.rxcare.qa.pages.components;

import com.rxcare.qa.api.models.PatientRequest;
import com.rxcare.qa.pages.BasePage;
import com.rxcare.qa.pages.PatientsPage;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.List;

/** Add / edit patient modal. */
public class PatientDialog extends BasePage {

    private static final By DIALOG = testId("patient-dialog");
    private static final By ERROR = testId("patient-form-error");

    public PatientDialog(WebDriver driver) {
        super(driver);
    }

    public PatientDialog waitUntilOpen() {
        visible(DIALOG);
        return this;
    }

    @Step("Fill patient form for {patient.lastName}, {patient.firstName}")
    public PatientDialog fill(PatientRequest patient) {
        type(testId("patient-first-name"), patient.firstName());
        type(testId("patient-last-name"), patient.lastName());
        type(testId("patient-dob"), patient.dateOfBirth());
        if (patient.gender() != null) {
            selectByValue(testId("patient-gender"), patient.gender());
        }
        type(testId("patient-phone"), patient.phone());
        type(testId("patient-email"), patient.email());
        type(testId("patient-address"), patient.address());
        type(testId("patient-insurance"), patient.insurancePlan());
        type(testId("patient-allergies"),
                patient.allergies() == null ? "" : String.join(", ", patient.allergies()));
        return this;
    }

    @Step("Save patient")
    public PatientsPage save() {
        markStale("patients-table");
        click(testId("patient-save"));
        waitUntilHidden(DIALOG);
        return new PatientsPage(driver).waitUntilLoaded();
    }

    @Step("Save patient (expecting validation errors)")
    public PatientDialog saveExpectingError() {
        click(testId("patient-save"));
        visible(ERROR);
        return this;
    }

    public String errorMessage() {
        return text(ERROR);
    }

    public List<String> errorDetails() {
        return driver.findElements(By.cssSelector("[data-testid='patient-form-error'] li"))
                .stream().map(e -> e.getText().trim()).toList();
    }

    public PatientsPage cancel() {
        click(testId("patient-cancel"));
        waitUntilHidden(DIALOG);
        return new PatientsPage(driver);
    }
}
