package com.merkatocircle.iqub.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Page Object for the Login page (/login).
 */
public class LoginPage extends BasePage {

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage open(String baseUrl) {
        driver.get(baseUrl + "/login");
        pause(500);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        pause(300);
        return this;
    }

    public LoginPage enterUsername(String email) {
        typeSlowly(driver.findElement(By.id("username")), email);
        pause(200);
        return this;
    }

    public LoginPage enterPassword(String password) {
        typeSlowly(driver.findElement(By.id("password")), password);
        pause(200);
        return this;
    }

    public DashboardPage submit() {
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        wait.until(ExpectedConditions.urlContains("/dashboard"));
        pause(600);
        return new DashboardPage(driver);
    }

    public LoginPage submitExpectingError() {
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".banner--error")));
        pause(400);
        return this;
    }

    public void assertTitleContains(String text) {
        assertThat(driver.getTitle()).contains(text);
    }
}
