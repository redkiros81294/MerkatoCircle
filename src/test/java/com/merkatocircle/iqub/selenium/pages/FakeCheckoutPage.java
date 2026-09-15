package com.merkatocircle.iqub.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Page Object for the Fake Checkout page (/test/fake-checkout).
 *
 * <p>The checkout form renders <em>buttons</em> (not radio inputs). The two "Simulate"
 * forms share the same {@code action}, so the button is located by the form's hidden
 * {@code outcome} value rather than by position.
 */
public class FakeCheckoutPage extends BasePage {

    public FakeCheckoutPage(WebDriver driver) {
        super(driver);
    }

    public PaymentReturnPage simulateSuccess() {
        return simulate("success");
    }

    public PaymentReturnPage simulateFailure() {
        return simulate("failed");
    }

    public void assertCheckoutUrl() {
        assertThat(driver.getCurrentUrl()).contains("/test/fake-checkout");
    }

    private PaymentReturnPage simulate(String outcome) {
        // Wait for the form itself rather than assuming the document is already parsed:
        // with pageLoadStrategy=EAGER the navigation returns at DOMContentLoaded.
        WebElement form = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//form[@action='/test/fake-checkout/simulate']"
                        + "//input[@name='outcome' and @value='" + outcome + "']/ancestor::form[1]")));
        WebElement button = wait.until(
                ExpectedConditions.elementToBeClickable(form.findElement(By.cssSelector("button"))));
        button.click();

        try {
            wait.until(d -> {
                String url = d.getCurrentUrl();
                return url.contains("/payments/return")
                        || url.contains("/login")
                        || url.contains("/dashboard");
            });
        } catch (org.openqa.selenium.TimeoutException e) {
            throw new AssertionError(describeFailure(outcome, "the browser never left the checkout page"), e);
        }

        String url = driver.getCurrentUrl();
        if (url.contains("/payments/return")) {
            return new PaymentReturnPage(driver);
        }
        if (url.contains("/login")) {
            throw new AssertionError(describeFailure(outcome,
                    "simulate redirected to /login - the session or CSRF token was gone"));
        }
        throw new AssertionError(describeFailure(outcome,
                "simulate bounced to /dashboard - an exception was caught in the return flow"));
    }

    private String describeFailure(String outcome, String what) {
        return "Simulating '" + outcome + "' did not reach /payments/return: " + what
                + "\n  URL:   " + driver.getCurrentUrl()
                + "\n  Title: " + driver.getTitle()
                + "\nPage source:\n" + driver.getPageSource();
    }
}