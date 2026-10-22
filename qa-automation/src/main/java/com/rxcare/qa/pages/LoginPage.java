package com.rxcare.qa.pages;

import com.rxcare.qa.config.Config;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class LoginPage extends BasePage {

    private static final By USERNAME = testId("username");
    private static final By PASSWORD = testId("password");
    private static final By SUBMIT = testId("login-button");
    private static final By ERROR = testId("login-error");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    @Step("Open the login page")
    public LoginPage open() {
        driver.get(Config.uiBaseUrl() + "/index.html");
        return waitUntilLoaded();
    }

    public LoginPage waitUntilLoaded() {
        wait.until(ExpectedConditions.urlContains("index.html"));
        visible(USERNAME);
        return this;
    }

    @Step("Sign in as {username}")
    public DashboardPage loginAs(String username, String password) {
        submit(username, password);
        return new DashboardPage(driver).waitUntilLoaded();
    }

    @Step("Attempt sign in as {username} (expecting an error)")
    public LoginPage loginExpectingError(String username, String password) {
        submit(username, password);
        visible(ERROR);
        return this;
    }

    public String errorMessage() {
        return text(ERROR);
    }

    public boolean isDisplayed() {
        return isPresentAndVisible(USERNAME) && isPresentAndVisible(SUBMIT);
    }

    private void submit(String username, String password) {
        type(USERNAME, username);
        type(PASSWORD, password);
        click(SUBMIT);
    }
}
