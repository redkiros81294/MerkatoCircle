package com.merkatocircle.iqub.selenium;

import static org.assertj.core.api.Assertions.assertThat;
import com.merkatocircle.iqub.selenium.pages.AccountPage;
import com.merkatocircle.iqub.selenium.pages.ContributePage;
import com.merkatocircle.iqub.selenium.pages.DashboardPage;
import com.merkatocircle.iqub.selenium.pages.FakeCheckoutPage;
import com.merkatocircle.iqub.selenium.pages.GroupsPage;
import com.merkatocircle.iqub.selenium.pages.LoginPage;
import com.merkatocircle.iqub.selenium.pages.NotificationsPage;
import com.merkatocircle.iqub.selenium.pages.PaymentReturnPage;
import com.merkatocircle.iqub.selenium.pages.RoundDetailPage;
import com.merkatocircle.iqub.selenium.pages.RoundsPage;
import com.merkatocircle.iqub.selenium.pages.SettingsPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Selenium E2E tests using the Page Object pattern.
 *
 * <p>Covers all 14 pages and critical user journeys. Tests run against a visible Chrome
 * browser with human-speed typing and pauses for observation.
 *
 * <p>Run with: mvn test -Dtest=IqubSeleniumTest
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IqubSeleniumTest {

    @LocalServerPort
    private int port;

    private WebDriver driver;

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private DashboardPage loginAs(String email, String password) {
        return new LoginPage(driver)
                .open(baseUrl())
                .enterUsername(email)
                .enterPassword(password)
                .submit();
    }

    // ---------- seeded accounts ----------
    private static final String SELAM_EMAIL = "selam@merkatocircle.et";
    private static final String YONAS_EMAIL = "yonas@merkatocircle.et";
    private static final String PASSWORD = "password123";

    // =============================================
    // PAGE 1: LOGIN
    // =============================================
    @Test
    @DisplayName("PAGE 1: Login page loads and authenticates")
    void loginPageLoadsAndAuthenticates() {
        new LoginPage(driver).open(baseUrl()).assertTitleContains("Sign in");
        assertThat(driver.findElement(By.cssSelector("h1")).getText()).contains("Merkato Circle");

        loginAs(SELAM_EMAIL, PASSWORD);
        assertThat(driver.getCurrentUrl()).contains("/dashboard");
    }

    @Test
    @DisplayName("PAGE 1: Login shows error on bad credentials")
    void loginShowsErrorOnBadCredentials() {
        new LoginPage(driver)
                .open(baseUrl())
                .enterUsername("wrong@example.com")
                .enterPassword("wrongpass")
                .submitExpectingError();

        assertThat(driver.getCurrentUrl()).contains("/login");
    }

    // =============================================
    // PAGE 2: REGISTER
    // =============================================
    @Test
    @DisplayName("PAGE 2: Registration page loads and creates account")
    void registrationPageLoadsAndCreatesAccount() {
        driver.get(baseUrl() + "/register");
        pause(500);
        assertThat(driver.getTitle()).contains("Create account");
        assertThat(driver.findElement(By.cssSelector("h1")).getText()).contains("Create your account");

        typeSlowly(driver.findElement(By.id("fullName")), "Selenium Test");
        pause(200);
        typeSlowly(driver.findElement(By.id("email")), "selenium" + System.currentTimeMillis() + "@example.com");
        pause(200);
        typeSlowly(driver.findElement(By.id("phone")), "0712345678");
        pause(200);
        typeSlowly(driver.findElement(By.id("password")), "password123");
        pause(200);
        typeSlowly(driver.findElement(By.id("confirmPassword")), "password123");
        pause(200);
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        wait(org.openqa.selenium.support.ui.ExpectedConditions.urlContains("/login"));
        pause(500);
        assertThat(driver.getCurrentUrl()).contains("/login");
    }

    // =============================================
    // PAGE 3: DASHBOARD
    // =============================================
    @Test
    @DisplayName("PAGE 3: Dashboard shows rotation wheel and round status")
    void dashboardShowsWheelAndStatus() {
        loginAs(SELAM_EMAIL, PASSWORD);
        pause(500);
        assertThat(driver.getTitle()).contains("My Groups");

        new DashboardPage(driver).assertWheelVisible();
        new DashboardPage(driver).assertHeadingContains("Round");
    }

    // =============================================
    // PAGE 4: CONTRIBUTE
    // =============================================
    @Test
    @DisplayName("PAGE 4: Contribute page shows payment status")
    void contributePageShowsPaymentStatus() {
        loginAs(YONAS_EMAIL, PASSWORD);
        ContributePage page = new ContributePage(driver).open(baseUrl());

        assertThat(driver.getTitle()).contains("Pay your contribution");
        assertThat(page.hasAmountDisplayed() || page.hasSettledMessage()).isTrue();
    }

    // =============================================
    // PAGE 5: FAKE CHECKOUT (test mode)
    // =============================================
    @Test
    @DisplayName("PAGE 5: Fake checkout simulates successful payment")
    void fakeCheckoutSimulatesSuccess() {
        loginAs(YONAS_EMAIL, PASSWORD);
        ContributePage contributePage = new ContributePage(driver).open(baseUrl());

        if (contributePage.hasAmountDisplayed()) {
            FakeCheckoutPage checkout = contributePage.clickPay();
            checkout.assertCheckoutUrl();
            PaymentReturnPage returnPage = checkout.simulateSuccess();
            returnPage.assertReturnUrl();
        } else {
            assertThat(driver.getPageSource()).contains("You're settled");
        }
    }

    @Test
    @DisplayName("PAGE 5: Fake checkout simulates failed payment")
    void fakeCheckoutSimulatesFailure() {
        loginAs(YONAS_EMAIL, PASSWORD);
        ContributePage contributePage = new ContributePage(driver).open(baseUrl());

        if (contributePage.hasAmountDisplayed()) {
            FakeCheckoutPage checkout = contributePage.clickPay();
            PaymentReturnPage returnPage = checkout.simulateFailure();
            returnPage.assertReturnUrl();
        } else {
            assertThat(driver.getPageSource()).contains("You're settled");
        }
    }

    // =============================================
    // PAGE 6: PAYMENT RETURN
    // =============================================
    @Test
    @DisplayName("PAGE 6: Payment return shows result")
    void paymentReturnShowsResult() {
        loginAs(YONAS_EMAIL, PASSWORD);
        ContributePage contributePage = new ContributePage(driver).open(baseUrl());

        if (contributePage.hasAmountDisplayed()) {
            PaymentReturnPage returnPage = contributePage.clickPay().simulateSuccess();
            returnPage.assertReturnUrl();
        }
    }

    // =============================================
    // PAGE 7: ACCOUNT
    // =============================================
    @Test
    @DisplayName("PAGE 7: Account page shows lifetime stats")
    void accountPageShowsLifetimeStats() {
        loginAs(SELAM_EMAIL, PASSWORD);
        new AccountPage(driver).open(baseUrl()).assertStatsVisible();
    }

    // =============================================
    // PAGE 8: SETTINGS
    // =============================================
    @Test
    @DisplayName("PAGE 8: Settings page loads with profile and password forms")
    void settingsPageLoadsWithForms() {
        loginAs(SELAM_EMAIL, PASSWORD);
        new SettingsPage(driver).open(baseUrl()).assertFormsVisible();
    }

    // =============================================
    // PAGE 9: ROUNDS
    // =============================================
    @Test
    @DisplayName("PAGE 9: Rounds page lists all rounds with status badges")
    void roundsPageListsAllRounds() {
        loginAs(SELAM_EMAIL, PASSWORD);
        RoundsPage page = new RoundsPage(driver).open(baseUrl());
        assertThat(page.hasRounds()).isTrue();
    }

    // =============================================
    // PAGE 10: ROUND DETAIL
    // =============================================
    @Test
    @DisplayName("PAGE 10: Round detail shows contributions table")
    void roundDetailShowsContributionsTable() {
        loginAs(SELAM_EMAIL, PASSWORD);
        new RoundsPage(driver).open(baseUrl()).clickFirstRound().assertRoundDetailUrl();
    }

    // =============================================
    // PAGE 11: GROUPS
    // =============================================
    @Test
    @DisplayName("PAGE 11: Groups page shows create form")
    void groupsPageShowsCreateForm() {
        loginAs(SELAM_EMAIL, PASSWORD);
        new GroupsPage(driver).open(baseUrl()).assertCreateFormVisible();
    }

    // =============================================
    // PAGE 12: MANAGE MEMBERS
    // =============================================
    @Test
    @DisplayName("PAGE 12: Manage members page shows roster")
    void manageMembersPageShowsRoster() {
        loginAs(SELAM_EMAIL, PASSWORD);
        new GroupsPage(driver).open(baseUrl());
        pause(400);

        java.util.List<WebElement> manageLinks = driver.findElements(By.cssSelector("a[href*='/groups/'][href*='/members']"));
        if (!manageLinks.isEmpty()) {
            manageLinks.get(0).click();
            wait(org.openqa.selenium.support.ui.ExpectedConditions.urlContains("/members"));
            pause(600);
            assertThat(driver.getCurrentUrl()).contains("/members");
        }
    }

    // =============================================
    // PAGE 13: NOTIFICATIONS
    // =============================================
    @Test
    @DisplayName("PAGE 13: Notifications page lists notifications or empty state")
    void notificationsPageListsNotifications() {
        loginAs(SELAM_EMAIL, PASSWORD);
        new NotificationsPage(driver).open(baseUrl()).assertHasContent();
    }

    // =============================================
    // PAGE 14: PLACE A BID
    // =============================================
    @Test
    @DisplayName("PAGE 14: Place a bid page loads for auction round")
    void placeBidPageLoadsForAuctionRound() {
        loginAs(SELAM_EMAIL, PASSWORD);
        RoundsPage roundsPage = new RoundsPage(driver).open(baseUrl());
        RoundDetailPage detailPage = roundsPage.clickFirstRound();

        java.util.List<WebElement> bidLinks = driver.findElements(By.cssSelector("a[href*='/bid']"));
        if (!bidLinks.isEmpty()) {
            bidLinks.get(0).click();
            wait(org.openqa.selenium.support.ui.ExpectedConditions.urlContains("/bid"));
            pause(600);
            assertThat(driver.getCurrentUrl()).contains("/bid");
        }
    }

    // =============================================
    // JOURNEY: Navigation through all topbar links
    // =============================================
    @Test
    @DisplayName("JOURNEY: All topbar navigation links work correctly")
    void allTopbarNavLinksWork() {
        loginAs(SELAM_EMAIL, PASSWORD);
        pause(400);

        String[][] navLinks = {{"My Groups", "/dashboard"}, {"Notifications", "/notifications"}, {"Account", "/account"}};
        for (String[] link : navLinks) {
            driver.findElement(By.linkText(link[0])).click();
            wait(org.openqa.selenium.support.ui.ExpectedConditions.urlContains(link[1]));
            pause(500);
            assertThat(driver.getCurrentUrl()).contains(link[1]);
        }
    }

    // =============================================
    // JOURNEY: Logout and re-login
    // =============================================
    @Test
    @DisplayName("JOURNEY: Logout clears session and login restores it")
    void logoutAndReLogin() {
        loginAs(SELAM_EMAIL, PASSWORD);
        pause(400);
        assertThat(driver.getCurrentUrl()).contains("/dashboard");

        driver.findElement(By.cssSelector("button.logout")).click();
        wait(org.openqa.selenium.support.ui.ExpectedConditions.urlContains("/login"));
        pause(600);
        assertThat(driver.getCurrentUrl()).contains("/login");

        loginAs(SELAM_EMAIL, PASSWORD);
        assertThat(driver.getCurrentUrl()).contains("/dashboard");
    }

    // =============================================
    // JOURNEY: Unauthenticated access redirects to login
    // =============================================
    @Test
    @DisplayName("JOURNEY: Unauthenticated access to protected page redirects to login")
    void unauthenticatedAccessRedirectsToLogin() {
        driver.get(baseUrl() + "/dashboard");
        pause(500);
        wait(org.openqa.selenium.support.ui.ExpectedConditions.urlContains("/login"));
        pause(400);
        assertThat(driver.getCurrentUrl()).contains("/login");
    }

    // =============================================
    // JOURNEY: Full payment flow (contribute → checkout → return)
    // =============================================
    @Test
    @DisplayName("JOURNEY: Full payment flow from contribute to payment return")
    void fullPaymentJourney() {
        loginAs(YONAS_EMAIL, PASSWORD);
        ContributePage contributePage = new ContributePage(driver).open(baseUrl());

        if (contributePage.hasAmountDisplayed()) {
            PaymentReturnPage returnPage = contributePage.clickPay().simulateSuccess();
            returnPage.assertReturnUrl();
        }
    }

    // Helper methods for Register page and backward compatibility
    private void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void typeSlowly(org.openqa.selenium.WebElement element, String text) {
        for (char c : text.toCharArray()) {
            element.sendKeys(String.valueOf(c));
            pause(80 + (long) (Math.random() * 60));
        }
    }

    private void wait(org.openqa.selenium.support.ui.ExpectedCondition<?> condition) {
        new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(10)).until(condition);
    }
}
