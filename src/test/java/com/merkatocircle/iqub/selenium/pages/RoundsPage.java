package com.merkatocircle.iqub.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Page Object for the Rounds page (/rounds).
 */
public class RoundsPage extends BasePage {

    public RoundsPage(WebDriver driver) {
        super(driver);
    }

    public RoundsPage open(String baseUrl) {
        driver.get(baseUrl + "/rounds");
        pause(500);
        wait.until(ExpectedConditions.urlContains("/rounds"));
        pause(500);
        return this;
    }

    public boolean hasRounds() {
        return !driver.findElements(By.cssSelector("table tbody tr")).isEmpty();
    }

    public RoundDetailPage clickFirstRound() {
        java.util.List<WebElement> rows = driver.findElements(By.cssSelector("table tbody tr.clickable"));
        if (!rows.isEmpty()) {
            rows.get(0).click();
            wait.until(ExpectedConditions.urlContains("/rounds/"));
            pause(600);
        }
        return new RoundDetailPage(driver);
    }
}
