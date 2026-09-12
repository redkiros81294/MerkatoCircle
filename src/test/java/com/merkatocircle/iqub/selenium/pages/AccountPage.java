package com.merkatocircle.iqub.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Page Object for the Account page (/account).
 */
public class AccountPage extends BasePage {

    public AccountPage(WebDriver driver) {
        super(driver);
    }

    public AccountPage open(String baseUrl) {
        driver.get(baseUrl + "/account");
        pause(500);
        wait.until(ExpectedConditions.urlContains("/account"));
        pause(500);
        return this;
    }

    public void assertStatsVisible() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("h1")));
        assertThat(driver.findElement(By.cssSelector("h1")).getText()).contains("Account");
        pause(300);
    }
}
