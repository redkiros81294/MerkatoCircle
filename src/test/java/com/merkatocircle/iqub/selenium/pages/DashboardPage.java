package com.merkatocircle.iqub.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Page Object for the Dashboard page (/dashboard).
 */
public class DashboardPage extends BasePage {

    public DashboardPage(WebDriver driver) {
        super(driver);
    }

    public void assertWheelVisible() {
        WebElement wheel = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("svg[aria-label*='Rotation wheel']")));
        assertThat(wheel).isNotNull();
        pause(400);
    }

    public void assertHeadingContains(String text) {
        assertThat(driver.findElement(By.cssSelector("h2")).getText()).contains(text);
    }
}
