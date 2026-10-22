package com.rxcare.qa.pages;

import com.rxcare.qa.config.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Shared Selenium plumbing for every page object: explicit waits only (no implicit waits,
 * no Thread.sleep), locators by stable data-testid attributes, and helpers for the
 * app's "data-state=loading|ready" table rendering contract.
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(Config.getInt("timeout.explicit.seconds", 10)));
        this.wait.ignoring(StaleElementReferenceException.class);
    }

    protected static By testId(String id) {
        return By.cssSelector("[data-testid='" + id + "']");
    }

    protected static By testId(String id, String extraCss) {
        return By.cssSelector("[data-testid='" + id + "']" + extraCss);
    }

    protected WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    protected void type(By locator, String text) {
        WebElement element = visible(locator);
        element.clear();
        if (text != null && !text.isEmpty()) {
            element.sendKeys(text);
        }
    }

    protected String text(By locator) {
        return visible(locator).getText().trim();
    }

    protected void selectByValue(By locator, String value) {
        new Select(visible(locator)).selectByValue(value);
    }

    protected boolean isPresentAndVisible(By locator) {
        List<WebElement> elements = driver.findElements(locator);
        return !elements.isEmpty() && elements.get(0).isDisplayed();
    }

    protected void waitUntilHidden(By locator) {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /** Clears data-state so the next wait cannot be satisfied by a stale "ready" from a previous render. */
    protected void markStale(String testId) {
        ((JavascriptExecutor) driver).executeScript(
                "var el = document.querySelector(\"[data-testid='" + testId + "']\");"
                        + " if (el) { el.removeAttribute('data-state'); }");
    }

    protected void waitForReady(String testId) {
        wait.until(ExpectedConditions.attributeToBe(testId(testId), "data-state", "ready"));
    }

    /** Runs a UI action that triggers a re-render of the given container, then waits for it to finish. */
    protected void rerender(String containerTestId, Runnable action) {
        markStale(containerTestId);
        action.run();
        waitForReady(containerTestId);
    }

    /** Hides any previous toast so the next wait reads the message produced by the next action. */
    protected void clearToast() {
        ((JavascriptExecutor) driver).executeScript(
                "var t = document.querySelector(\"[data-testid='toast']\");"
                        + " if (t) { t.textContent = ''; t.classList.add('hidden'); }");
    }

    public String waitForToastContaining(String expected) {
        wait.until(ExpectedConditions.textToBePresentInElementLocated(testId("toast"), expected));
        return text(testId("toast"));
    }

    public boolean isToastError() {
        String cssClass = visible(testId("toast")).getDomAttribute("class");
        return cssClass != null && cssClass.contains("error");
    }

    public String currentUrl() {
        return driver.getCurrentUrl();
    }
}
