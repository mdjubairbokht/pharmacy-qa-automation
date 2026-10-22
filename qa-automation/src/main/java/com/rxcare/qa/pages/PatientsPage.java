package com.rxcare.qa.pages;

import com.rxcare.qa.pages.components.PatientDialog;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Optional;

public class PatientsPage extends AppPage {

    private static final String TABLE = "patients-table";
    private static final By SEARCH_INPUT = testId("patient-search-input");
    private static final By SEARCH_BUTTON = testId("patient-search-button");
    private static final By ADD_BUTTON = testId("add-patient-button");
    private static final By ROWS = testId("patient-row");
    private static final By EMPTY = testId("patients-empty");

    public PatientsPage(WebDriver driver) {
        super(driver);
    }

    public PatientsPage waitUntilLoaded() {
        waitForReady(TABLE);
        return this;
    }

    @Step("Search patients for '{term}'")
    public PatientsPage search(String term) {
        type(SEARCH_INPUT, term);
        rerender(TABLE, () -> click(SEARCH_BUTTON));
        return this;
    }

    @Step("Open the Add patient dialog")
    public PatientDialog openAddPatient() {
        click(ADD_BUTTON);
        return new PatientDialog(driver).waitUntilOpen();
    }

    public int rowCount() {
        return driver.findElements(ROWS).size();
    }

    public boolean isEmptyStateShown() {
        return isPresentAndVisible(EMPTY);
    }

    public List<String> displayedNames() {
        return driver.findElements(By.cssSelector("[data-testid='patient-row'] td[data-col='name']"))
                .stream().map(e -> e.getText().trim()).toList();
    }

    /** Row whose name cell is exactly "Last, First". */
    public Optional<WebElement> rowFor(String displayName) {
        return driver.findElements(ROWS).stream()
                .filter(row -> row.findElement(By.cssSelector("td[data-col='name']")).getText().trim()
                        .equals(displayName))
                .findFirst();
    }

    public List<String> allergiesFor(String displayName) {
        WebElement row = rowFor(displayName)
                .orElseThrow(() -> new AssertionError("Patient row not found: " + displayName));
        return row.findElements(By.cssSelector("td[data-col='allergies'] .chip"))
                .stream().map(e -> e.getText().trim()).toList();
    }

    @Step("Edit patient {displayName}")
    public PatientDialog edit(String displayName) {
        WebElement row = rowFor(displayName)
                .orElseThrow(() -> new AssertionError("Patient row not found: " + displayName));
        row.findElement(By.cssSelector("[data-testid='patient-edit']")).click();
        return new PatientDialog(driver).waitUntilOpen();
    }
}
