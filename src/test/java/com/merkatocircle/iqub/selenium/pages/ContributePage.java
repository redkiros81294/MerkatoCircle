package com.merkatocircle.iqub.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

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
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("main")));
        return this;
    }

    public boolean hasAmountDisplayed() {
        return !driver.findElements(By.cssSelector(".amount")).isEmpty();
    }

    public boolean hasSettledMessage() {
        return driver.getPageSource().contains("You're settled");
    }

    /**
     * Clicks Pay and waits for the browser to land on the fake checkout page.
     *
     * <p>The interesting failure modes all look identical from the outside ("the URL never
     * became /test/fake-checkout"), so each one is detected and named here instead of being
     * reported as a bare timeout:
     * <ul>
     *   <li>a bounce back to /contribute - a business-rule exception (AlreadyPaidException,
     *       RoundClosedException) that GlobalExceptionHandler redirected to the Referer;</li>
     *   <li>a bounce to /dashboard - an IllegalState/IllegalArgument lookup failure;</li>
     *   <li>staying on /contribute/pay - an unhandled 4xx/5xx, most often a CSRF 403;</li>
     *   <li>a redirect to /login - the session went away mid-form.</li>
     * </ul>
     */
    public FakeCheckoutPage clickPay() {
        String startUrl = driver.getCurrentUrl();

        WebElement button = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.cssSelector("form[action='/contribute/pay'] button")));
        button.click();

        try {
            wait.until(d -> {
                String url = d.getCurrentUrl();
                return url.contains("/test/fake-checkout")
                        || url.contains("/login")
                        || url.contains("/dashboard")
                        || url.contains("/contribute/pay")
                        || (url.contains("/contribute") && !url.equals(startUrl));
            });
        } catch (org.openqa.selenium.TimeoutException e) {
            throw new AssertionError(describeFailure("the browser never left the contribute page"), e);
        }

        String url = driver.getCurrentUrl();
        if (url.contains("/test/fake-checkout")) {
            return new FakeCheckoutPage(driver);
        }
        if (url.contains("/login")) {
            throw new AssertionError(describeFailure(
                    "Pay redirected to /login - the session or CSRF token was gone by the time the form posted"));
        }
        if (url.contains("/contribute/pay")) {
            throw new AssertionError(describeFailure(
                    "POST /contribute/pay returned an error page instead of a redirect "
                            + "(most likely a 403 CSRF rejection or an unhandled 500)"));
        }
        if (url.contains("/dashboard")) {
            throw new AssertionError(describeFailure(
                    "Pay bounced to /dashboard - GlobalExceptionHandler caught an "
                            + "IllegalStateException/IllegalArgumentException in the pay flow"));
        }
        throw new AssertionError(describeFailure(
                "Pay bounced back to /contribute - GlobalExceptionHandler redirected a "
                        + "business-rule exception (AlreadyPaidException / RoundClosedException) "
                        + "to the Referer"));
    }

    private String describeFailure(String what) {
        String banner = driver.findElements(By.cssSelector(".banner, .banner--error, .alert")).stream()
                .map(WebElement::getText)
                .filter(t -> !t.isBlank())
                .findFirst()
                .orElse("(no banner on the page)");
        return "Pay did not reach /test/fake-checkout: " + what
                + "\n  URL:    " + driver.getCurrentUrl()
                + "\n  Banner: " + banner
                + "\n  Title:  " + driver.getTitle()
                + "\nPage source:\n" + driver.getPageSource();
    }
}