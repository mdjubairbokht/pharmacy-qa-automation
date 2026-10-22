package com.rxcare.qa.listeners;

import com.rxcare.qa.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Properties;

/**
 * Before any test runs: waits for the application's /api/health endpoint (fail fast with a
 * clear message instead of hundreds of connection errors) and writes Allure's
 * environment.properties so every report shows where and how it was produced.
 */
public class SuiteListener implements ISuiteListener {

    private static final Logger LOG = LoggerFactory.getLogger(SuiteListener.class);

    @Override
    public void onStart(ISuite suite) {
        writeAllureEnvironment();
        waitForApplication();
    }

    private void waitForApplication() {
        String url = Config.apiBaseUrl() + "/api/health";
        int timeoutSeconds = Config.getInt("health.timeout.seconds", 90);
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        String lastProblem = "no response";

        while (System.currentTimeMillis() < deadline) {
            try {
                HttpResponse<String> response = client.send(
                        HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(3)).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200 && response.body().contains("\"database\":\"UP\"")) {
                    LOG.info("Application is healthy at {}", url);
                    return;
                }
                lastProblem = "HTTP " + response.statusCode() + " " + response.body();
            } catch (IOException e) {
                lastProblem = e.getClass().getSimpleName() + ": " + e.getMessage();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for the application", e);
            }
            pause();
        }
        throw new IllegalStateException("Application not healthy at " + url + " after " + timeoutSeconds
                + "s (last result: " + lastProblem + "). Start it first: see README 'Run locally'.");
    }

    private static void pause() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void writeAllureEnvironment() {
        Path dir = Paths.get(System.getProperty("allure.results.directory", "target/allure-results"));
        Properties env = new Properties();
        env.setProperty("Environment", Config.env());
        env.setProperty("App.URL", Config.apiBaseUrl());
        env.setProperty("UI.URL", Config.uiBaseUrl());
        env.setProperty("Browser", Config.get("browser", "chrome") + (Config.getBool("headless", false) ? " (headless)" : ""));
        env.setProperty("Grid", Config.get("grid.url", "").isBlank() ? "local" : Config.get("grid.url"));
        env.setProperty("Java", System.getProperty("java.version"));
        try {
            Files.createDirectories(dir);
            try (OutputStream out = Files.newOutputStream(dir.resolve("environment.properties"))) {
                env.store(out, "Allure environment");
            }
        } catch (IOException e) {
            LOG.warn("Could not write Allure environment.properties: {}", e.getMessage());
        }
    }
}
