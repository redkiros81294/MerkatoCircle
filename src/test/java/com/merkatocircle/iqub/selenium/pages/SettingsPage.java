package com.merkatocircle.iqub.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Page Object for the Settings page (/settings).
 */
public class SettingsPage extends BasePage {

    public SettingsPage(WebDriver driver) {
        super(driver);
    }

    public SettingsPage open(String baseUrl) {
        driver.get(baseUrl + "/settings");
        pause(500);
        wait.until(ExpectedConditions.urlContains("/settings"));
        pause(500);
        return this;
    }

    public void assertFormsVisible() {
        assertThat(driver.findElement(By.cssSelector("h2")).getText()).contains("Settings");
        pause(300);
        assertThat(driver.findElement(By.id("fullName")).isDisplayed()).isTrue();
        assertThat(driver.findElement(By.id("phone")).isDisplayed()).isTrue();
        assertThat(driver.findElement(By.id("currentPassword")).isDisplayed()).isTrue();
        assertThat(driver.findElement(By.id("newPassword")).isDisplayed()).isTrue();
    }
}
