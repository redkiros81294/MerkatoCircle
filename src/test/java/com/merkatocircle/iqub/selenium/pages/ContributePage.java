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
        wait.until(ExpectedConditions.urlContains("/contribute"));
        return this;
    }

    public boolean hasAmountDisplayed() {
        return driver.findElements(By.cssSelector(".amount")).size() > 0;
    }

    public boolean hasSettledMessage() {
        return driver.getPageSource().contains("You're settled");
    }

    /**
     * Clicks Pay and waits for the browser to land on the fake checkout page.
     *
     * <p>The button is waited on explicitly (present + clickable) before the click, and
     * the post-click wait accepts <em>either</em> the checkout URL or a login redirect.
     * If neither happens within the timeout the failure message includes the actual URL
     * and page source so the next investigation does not start from a bare timeout.
     */
    public FakeCheckoutPage clickPay() {
        WebElement button = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.cssSelector("form[action='/contribute/pay'] button")));
        button.click();

        boolean navigated;
        try {
            navigated = wait.until(d -> {
                String url = driver.getCurrentUrl();
                return url.contains("/test/fake-checkout") || url.contains("/login");
            });
        } catch (org.openqa.selenium.TimeoutException e) {
            throw new AssertionError("Timed out waiting for navigation after clicking Pay. "
                    + "URL: " + driver.getCurrentUrl()
                    + "\nPage source:\n" + driver.getPageSource(), e);
        }

        String url = driver.getCurrentUrl();
        if (url.contains("/login")) {
            throw new AssertionError("Pay button redirected to /login — session or CSRF "
                    + "token likely expired mid-form. URL: " + url);
        }
        if (!navigated) {
            throw new AssertionError("Expected navigation to /test/fake-checkout but the "
                    + "browser stayed on the contribute page. URL: " + url
                    + "\nPage source:\n" + driver.getPageSource());
        }
        return new FakeCheckoutPage(driver);
    }
}