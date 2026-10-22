package com.rxcare.qa.driver;

import com.rxcare.qa.config.Config;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Creates WebDriver instances for chrome | firefox | edge, locally or on a Selenium Grid
 * (grid.url). Driver binaries are resolved automatically by Selenium Manager.
 */
public final class DriverFactory {

    private static final String WINDOW_SIZE = "1440,900";

    private DriverFactory() {
    }

    public static WebDriver create() {
        String browser = Config.get("browser", "chrome").toLowerCase(Locale.ROOT);
        boolean headless = Config.getBool("headless", false);
        String gridUrl = Config.get("grid.url", "");

        Capabilities options = switch (browser) {
            case "firefox" -> firefoxOptions(headless);
            case "edge" -> edgeOptions(headless);
            case "chrome" -> chromeOptions(headless);
            default -> throw new IllegalArgumentException("Unsupported browser: " + browser);
        };

        WebDriver driver = gridUrl.isBlank() ? local(browser, options) : remote(gridUrl, options);
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(Config.getInt("timeout.pageLoad.seconds", 30)));
        if (!headless && gridUrl.isBlank()) {
            driver.manage().window().maximize();
        }
        return driver;
    }

    private static WebDriver local(String browser, Capabilities options) {
        return switch (browser) {
            case "firefox" -> new FirefoxDriver((FirefoxOptions) options);
            case "edge" -> new EdgeDriver((EdgeOptions) options);
            default -> new ChromeDriver((ChromeOptions) options);
        };
    }

    private static WebDriver remote(String gridUrl, Capabilities options) {
        try {
            return new RemoteWebDriver(URI.create(gridUrl).toURL(), options);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid grid.url: " + gridUrl, e);
        }
    }

    private static ChromeOptions chromeOptions(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=" + WINDOW_SIZE, "--disable-dev-shm-usage", "--no-sandbox",
                "--disable-search-engine-choice-screen");
        if (headless) {
            options.addArguments("--headless=new");
        }
        // Keep Chrome's password manager / breach-detection popups from covering the app.
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.password_manager_leak_detection", false);
        options.setExperimentalOption("prefs", prefs);
        String binary = Config.get("chrome.binary", "");
        if (!binary.isBlank()) {
            options.setBinary(binary);
        }
        return options;
    }

    private static FirefoxOptions firefoxOptions(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--width=1440", "--height=900");
        if (headless) {
            options.addArguments("-headless");
        }
        return options;
    }

    private static EdgeOptions edgeOptions(boolean headless) {
        EdgeOptions options = new EdgeOptions();
        options.addArguments("--window-size=" + WINDOW_SIZE);
        if (headless) {
            options.addArguments("--headless=new");
        }
        return options;
    }
}
