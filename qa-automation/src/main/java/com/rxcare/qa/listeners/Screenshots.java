package com.rxcare.qa.listeners;

import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Captures evidence of a failed UI test: screenshot + URL + page source, saved to disk and attached to Allure. */
public final class Screenshots {

    private static final Logger LOG = LoggerFactory.getLogger(Screenshots.class);

    private Screenshots() {
    }

    public static void captureFailure(WebDriver driver, String testName) {
        try {
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Allure.addAttachment("Screenshot on failure", "image/png", new ByteArrayInputStream(png), ".png");
            Allure.addAttachment("URL", "text/plain", driver.getCurrentUrl());
            Allure.addAttachment("Page source", "text/html", driver.getPageSource());

            Path dir = Paths.get("target", "screenshots");
            Files.createDirectories(dir);
            Path file = dir.resolve(testName.replaceAll("[^A-Za-z0-9._-]", "_") + "_" + System.currentTimeMillis() + ".png");
            Files.write(file, png);
            LOG.info("Saved failure screenshot to {}", file.toAbsolutePath());
        } catch (IOException | RuntimeException e) {
            LOG.warn("Could not capture failure evidence for {}: {}", testName, e.getMessage());
        }
    }
}
