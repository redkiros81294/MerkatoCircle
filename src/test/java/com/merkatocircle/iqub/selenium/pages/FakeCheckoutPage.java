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
 * {@code outcome} value rather than by position. Clicking is waited on explicitly and
 * the post-click wait accepts either the return URL or a login redirect; on a stuck
 * navigation the failure message includes the URL and page source so the cause is
 * visible without re-running with extra logging.
 */
public class FakeCheckoutPage extends BasePage {

    public FakeCheckoutPage(WebDriver driver) {
        super(driver);
    }

    public PaymentReturnPage simulateSuccess() {
        return simulate("success", "/payments/return");
    }

    public PaymentReturnPage simulateFailure() {
        return simulate("failed", "/payments/return");
    }

    public void assertCheckoutUrl() {
        assertThat(driver.getCurrentUrl()).contains("/test/fake-checkout");
    }

    private PaymentReturnPage simulate(String outcome, String expectedFragment) {
        // The two forms differ only by their hidden outcome value, so locate the form
        // that carries the requested outcome and click its submit button.
        WebElement form = driver.findElement(By.xpath(
                "//form[@action='/test/fake-checkout/simulate']"
                        + "//input[@name='outcome' and @value='" + outcome + "']/ancestor::form[1]"));
        WebElement button = wait.until(
                ExpectedConditions.elementToBeClickable(form.findElement(By.cssSelector("button"))));
        button.click();

        boolean navigated = wait.until(d -> {
            String url = driver.getCurrentUrl();
            return url.contains(expectedFragment) || url.contains("/login");
        });

        String url = driver.getCurrentUrl();
        if (url.contains("/login")) {
            throw new AssertionError("Simulate button redirected to /login — session or CSRF "
                    + "token likely expired mid-form. URL: " + url);
        }
        if (!navigated) {
            throw new AssertionError("Expected navigation to " + expectedFragment
                    + " after simulating '" + outcome + "' but the browser did not move. "
                    + "URL: " + url + "\nPage source:\n" + driver.getPageSource());
        }
        return new PaymentReturnPage(driver);
    }
}
