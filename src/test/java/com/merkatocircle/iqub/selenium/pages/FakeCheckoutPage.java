package com.merkatocircle.iqub.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Page Object for the Fake Checkout page (/test/fake-checkout).
 */
public class FakeCheckoutPage extends BasePage {

    public FakeCheckoutPage(WebDriver driver) {
        super(driver);
    }

    public PaymentReturnPage simulateSuccess() {
        driver.findElement(By.cssSelector("input[value='success']")).findElement(By.xpath("..")).click();
        wait.until(ExpectedConditions.urlContains("/payments/return"));
        pause(600);
        return new PaymentReturnPage(driver);
    }

    public PaymentReturnPage simulateFailure() {
        driver.findElement(By.cssSelector("input[value='failure']")).findElement(By.xpath("..")).click();
        wait.until(ExpectedConditions.urlContains("/payments/return"));
        pause(600);
        return new PaymentReturnPage(driver);
    }

    public void assertCheckoutUrl() {
        assertThat(driver.getCurrentUrl()).contains("/test/fake-checkout");
    }
}
