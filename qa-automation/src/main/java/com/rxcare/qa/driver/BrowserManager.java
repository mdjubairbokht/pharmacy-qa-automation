package com.rxcare.qa.driver;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One WebDriver per test thread, so UI test classes can run in parallel
 * (TestNG parallel="classes") without sharing a browser.
 */
public final class BrowserManager {

    private static final Logger LOG = LoggerFactory.getLogger(BrowserManager.class);
    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private BrowserManager() {
    }

    public static WebDriver start() {
        quit();
        WebDriver driver = DriverFactory.create();
        DRIVER.set(driver);
        LOG.info("Started browser on thread {}", Thread.currentThread().getName());
        return driver;
    }

    public static WebDriver driver() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException("No browser started on this thread. Did the test extend BaseUiTest?");
        }
        return driver;
    }

    public static boolean isActive() {
        return DRIVER.get() != null;
    }

    public static void quit() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            try {
                driver.quit();
            } catch (RuntimeException e) {
                LOG.warn("Error while closing browser: {}", e.getMessage());
            } finally {
                DRIVER.remove();
            }
        }
    }
}
