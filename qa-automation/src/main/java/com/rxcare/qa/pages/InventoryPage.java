package com.rxcare.qa.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class InventoryPage extends AppPage {

    private static final String TABLE = "inventory-table";
    private static final By LOW_ONLY = testId("low-stock-only");

    public InventoryPage(WebDriver driver) {
        super(driver);
    }

    public InventoryPage waitUntilLoaded() {
        waitForReady(TABLE);
        return this;
    }

    private static By row(String ndc) {
        return By.cssSelector("[data-testid='inventory-row'][data-ndc='" + ndc + "']");
    }

    @Step("Toggle 'Low stock only' to {enabled}")
    public InventoryPage showLowStockOnly(boolean enabled) {
        WebElement checkbox = visible(LOW_ONLY);
        if (checkbox.isSelected() != enabled) {
            rerender(TABLE, checkbox::click);
        }
        return this;
    }

    public List<String> displayedNdcs() {
        return driver.findElements(By.cssSelector("[data-testid='inventory-row']"))
                .stream().map(e -> e.getDomAttribute("data-ndc")).toList();
    }

    public int qtyOnHand(String ndc) {
        return Integer.parseInt(visible(row(ndc)).findElement(By.cssSelector("td[data-col='qty']")).getText().trim());
    }

    public boolean isFlaggedLowStock(String ndc) {
        return !visible(row(ndc)).findElements(By.cssSelector("[data-testid='low-stock-badge']")).isEmpty();
    }

    @Step("Receive {quantity} units of {ndc} (ref {reference})")
    public InventoryPage receive(String ndc, int quantity, String reference) {
        selectByValue(testId("receive-drug"), ndc);
        type(testId("receive-qty"), String.valueOf(quantity));
        type(testId("receive-reason"), reference);
        markStale(TABLE);
        clearToast();
        click(testId("receive-submit"));
        waitForToastContaining("Received " + quantity + " units");
        waitForReady(TABLE);
        return this;
    }
}
