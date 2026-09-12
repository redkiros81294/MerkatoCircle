package com.merkatocircle.iqub.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Page Object for the Payment Return page (/payments/return).
 */
public class PaymentReturnPage extends BasePage {

    public PaymentReturnPage(WebDriver driver) {
        super(driver);
    }

    public void assertReturnUrl() {
        assertThat(driver.getCurrentUrl()).contains("/payments/return");
    }
}
