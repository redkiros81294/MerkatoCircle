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
        pause(50);
        wait.until(ExpectedConditions.urlContains("/rounds"));
        pause(50);
        return this;
    }

    public boolean hasRounds() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table")));
        } catch (Exception e) {
            return false;
        }
        return !driver.findElements(By.cssSelector("table tbody tr")).isEmpty();
    }

    public RoundDetailPage clickFirstRound() {
        java.util.List<WebElement> rows = driver.findElements(By.cssSelector("table tbody tr"));
        for (WebElement row : rows) {
            if (!row.findElements(By.cssSelector("a")).isEmpty()) {
                row.findElement(By.cssSelector("a")).click();
                wait.until(ExpectedConditions.urlContains("/rounds/"));
                pause(50);
                break;
            }
        }
        return new RoundDetailPage(driver);
    }
}
