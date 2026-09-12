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
        assertThat(driver.findElement(By.cssSelector("h2")).getText()).contains("Your account");
        pause(300);
        assertThat(driver.findElements(By.cssSelector(".stat3 .card")).size()).isEqualTo(3);
    }
}
