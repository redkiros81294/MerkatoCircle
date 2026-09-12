package com.merkatocircle.iqub.selenium.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Base page object with common navigation and wait utilities.
 */
abstract class BasePage {
    protected final WebDriver driver;
    protected final WebDriverWait wait;

    BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(10));
    }

    protected void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected void typeSlowly(org.openqa.selenium.WebElement element, String text) {
        for (char c : text.toCharArray()) {
            element.sendKeys(String.valueOf(c));
            pause(80 + (long) (Math.random() * 60));
        }
    }

    protected void clickAndWait(org.openqa.selenium.WebElement element, String urlFragment) {
        element.click();
        wait.until(org.openqa.selenium.support.ui.ExpectedConditions.urlContains(urlFragment));
        pause(600);
    }
}
