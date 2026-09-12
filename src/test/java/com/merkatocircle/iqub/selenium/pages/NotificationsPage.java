package com.merkatocircle.iqub.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Page Object for the Notifications page (/notifications).
 */
public class NotificationsPage extends BasePage {

    public NotificationsPage(WebDriver driver) {
        super(driver);
    }

    public NotificationsPage open(String baseUrl) {
        driver.get(baseUrl + "/notifications");
        pause(500);
        wait.until(ExpectedConditions.urlContains("/notifications"));
        pause(500);
        return this;
    }

    public void assertHasContent() {
        assertThat(driver.getTitle()).contains("Notifications");
        pause(300);
        boolean hasNotifications = driver.findElements(By.cssSelector(".card.card--tight")).size() > 0;
        boolean hasEmptyState = driver.getPageSource().contains("Nothing yet");
        assertThat(hasNotifications || hasEmptyState).isTrue();
    }
}
