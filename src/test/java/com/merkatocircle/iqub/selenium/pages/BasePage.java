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
        this.wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(20));
    }

    /**
     * Pauses execution for human-speed observation in local debug mode.
     * In CI/headless mode, these pauses add ~200s across all tests.
     * Original pause durations: 300-600ms per call, 80-140ms per character typed.
     * Reduced to 10ms for CI compatibility while preserving test correctness.
     */
    protected void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Types text character-by-character. Original: 80-140ms per char (human speed).
     * Reduced to 10ms per char for CI timeout compliance.
     */
    protected void typeSlowly(org.openqa.selenium.WebElement element, String text) {
        for (char c : text.toCharArray()) {
            element.sendKeys(String.valueOf(c));
            pause(10);
        }
    }

    protected void clickAndWait(org.openqa.selenium.WebElement element, String urlFragment) {
        element.click();
        wait.until(org.openqa.selenium.support.ui.ExpectedConditions.urlContains(urlFragment));
        pause(100);
    }
}
