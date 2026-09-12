package com.merkatocircle.iqub.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Page Object for the Groups page (/groups).
 */
public class GroupsPage extends BasePage {

    public GroupsPage(WebDriver driver) {
        super(driver);
    }

    public GroupsPage open(String baseUrl) {
        driver.get(baseUrl + "/groups");
        pause(500);
        wait.until(ExpectedConditions.urlContains("/groups"));
        pause(500);
        return this;
    }

    public void assertCreateFormVisible() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("name")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("contributionAmount")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("roundIntervalDays")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("maxMembers")));
        assertThat(driver.findElement(By.id("name")).isDisplayed()).isTrue();
        assertThat(driver.findElement(By.id("contributionAmount")).isDisplayed()).isTrue();
        assertThat(driver.findElement(By.id("roundIntervalDays")).isDisplayed()).isTrue();
        assertThat(driver.findElement(By.id("maxMembers")).isDisplayed()).isTrue();
    }
}
