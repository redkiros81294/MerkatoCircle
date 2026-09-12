package com.merkatocircle.iqub.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Page Object for the Contribute page (/contribute).
 */
public class ContributePage extends BasePage {

    public ContributePage(WebDriver driver) {
        super(driver);
    }

    public ContributePage open(String baseUrl) {
        driver.get(baseUrl + "/contribute");
        pause(500);
        wait.until(ExpectedConditions.urlContains("/contribute"));
        pause(500);
        return this;
    }

    public boolean hasAmountDisplayed() {
        return driver.findElements(By.cssSelector(".amount")).size() > 0;
    }

    public boolean hasSettledMessage() {
        return driver.getPageSource().contains("You're settled");
    }

    public FakeCheckoutPage clickPay() {
        driver.findElement(By.cssSelector("form[action='/contribute/pay'] button")).click();
        wait.until(ExpectedConditions.urlContains("/test/fake-checkout"));
        pause(600);
        return new FakeCheckoutPage(driver);
    }
}
