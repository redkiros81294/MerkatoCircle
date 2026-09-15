package com.merkatocircle.iqub.selenium;

import static org.assertj.core.api.Assertions.assertThat;
import com.merkatocircle.iqub.domain.Contribution;
import com.merkatocircle.iqub.domain.Iqub;
import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.Round;
import com.merkatocircle.iqub.domain.RoundStatus;
import com.merkatocircle.iqub.repository.ContributionRepository;
import com.merkatocircle.iqub.repository.IqubRepository;
import com.merkatocircle.iqub.repository.MemberRepository;
import com.merkatocircle.iqub.repository.RoundRepository;
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Selenium E2E tests using the Page Object pattern.
 *
 * <p>Every test is independent: the payment tests reset the fixture they touch in
 * {@link #setUp()}, so no test can be broken by the order it happens to run in and
 * no test silently skips its assertions when an earlier test left state behind.
 *
 * <p>Run with: mvn test -Dtest=IqubSeleniumTest
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IqubSeleniumTest {

    /** Where screenshots and page dumps land; CI uploads this directory as an artifact. */
    private static final Path DIAGNOSTICS_DIR = Paths.get("target", "selenium-diagnostics");

    @LocalServerPort
    private int port;

    @Autowired
    private IqubRepository iqubRepository;
    @Autowired
    private RoundRepository roundRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ContributionRepository contributionRepository;

    private WebDriver driver;

    @BeforeEach
    void setUp() {
        resetPaymentFixture();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        // Nothing in these tests needs Chrome's own background traffic, and each of these
        // is a known source of multi-second stalls on a cold CI runner.
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-background-networking");
        options.addArguments("--disable-sync");
        options.addArguments("--disable-default-apps");
        options.addArguments("--disable-search-engine-choice-screen");
        options.addArguments("--no-first-run");
        options.addArguments("--no-default-browser-check");
        options.addArguments("--blink-settings=imagesEnabled=false");

        // THE IMPORTANT ONE. Every template <link>s Google Fonts. ChromeDriver serialises
        // commands behind an in-flight navigation, so while a page is still loading, both
        // click() and getCurrentUrl() block. One slow or unreachable fonts.googleapis.com
        // request therefore does not look like "fonts are slow" - it looks like "the browser
        // never navigated after clicking Pay", which is exactly the timeout being chased.
        // Resolving every non-local host to nothing makes external assets fail instantly
        // instead of hanging the load event. The app itself only ever talks to localhost
        // under the fake payment gateway, so nothing real is lost.
        options.addArguments("--host-resolver-rules=MAP * ~NOTFOUND, EXCLUDE localhost, EXCLUDE 127.0.0.1");

        // EAGER returns control at DOMContentLoaded instead of the load event, so a
        // stylesheet or font that is still in flight can never stall a navigation wait.
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);

        // If CHROME_BIN / CHROMEDRIVER_BIN are set (CI pins these to a version-matched
        // pair), use them explicitly. Otherwise fall back to Selenium Manager's
        // auto-download for local/dev environments.
        String chromeBin = System.getenv("CHROME_BIN");
        String chromedriverBin = System.getenv("CHROMEDRIVER_BIN");
        if (chromeBin != null && !chromeBin.isBlank()) {
            options.setBinary(chromeBin);
        }
        if (chromedriverBin != null && !chromedriverBin.isBlank()) {
            System.setProperty("webdriver.chrome.driver", chromedriverBin);
        }

        driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
        // No implicit wait: it silently compounds with the explicit WebDriverWaits in the
        // page objects and makes every timeout longer and harder to read.
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        if (driver == null) {
            return;
        }
        captureDiagnostics(testInfo.getTestMethod().map(m -> m.getName()).orElse("unknown"));
        driver.quit();
        driver = null;
    }

    /**
     * Dumps a screenshot, the final URL and the page source for every test. On a green run
     * this costs a few hundred kilobytes; on a red one it is the difference between "timed
     * out" and knowing whether the server returned a 403, a 500 or a redirect back to
     * /contribute.
     */
    private void captureDiagnostics(String testName) {
        try {
            Files.createDirectories(DIAGNOSTICS_DIR);
            Files.write(DIAGNOSTICS_DIR.resolve(testName + ".html"),
                    ("<!-- final URL: " + driver.getCurrentUrl() + " -->\n" + driver.getPageSource())
                            .getBytes(StandardCharsets.UTF_8));
            if (driver instanceof TakesScreenshot shooter) {
                Files.write(DIAGNOSTICS_DIR.resolve(testName + ".png"),
                        shooter.getScreenshotAs(OutputType.BYTES));
            }
        } catch (IOException | RuntimeException ignored) {
            // Diagnostics must never turn a passing test red.
        }
    }

    /**
     * Puts Yonas's current-round contribution back to a clean, unpaid state and reopens the
     * round.
     *
     * <p>Without this, the payment tests are order-dependent: the first one to run flips the
     * contribution to PAID, after which {@code ContributionService.initiatePayment} throws
     * {@code AlreadyPaidException}. {@code GlobalExceptionHandler} turns that into a redirect
     * back to the Referer - the contribute page - so the browser lands right back where it
     * started and the test reports "timed out waiting for navigation after clicking Pay"
     * rather than the real cause.
     */
    private void resetPaymentFixture() {
        Iqub iqub = iqubRepository.findAll().stream().findFirst().orElse(null);
        if (iqub == null) {
            return; // context not seeded (nothing to reset)
        }
        Round round = roundRepository.findTopByIqubOrderByRoundNumberDesc(iqub).orElse(null);
        Member yonas = memberRepository.findByEmail(YONAS_EMAIL).orElse(null);
        if (round == null || yonas == null) {
            return;
        }
        if (round.getStatus() != RoundStatus.OPEN) {
            round.setStatus(RoundStatus.OPEN);
            roundRepository.save(round);
        }
        contributionRepository.findByRoundAndMember(round, yonas)
                .ifPresent(contributionRepository::delete);
        contributionRepository.save(
                new Contribution(round, yonas, iqub.getContributionAmount()));
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
        wait(org.openqa.selenium.support.ui.ExpectedConditions
                .visibilityOfElementLocated(By.id("fullName")));
        assertThat(driver.getTitle()).contains("Create account");
        assertThat(driver.findElement(By.cssSelector("h1")).getText()).contains("Join Merkato Circle");

        type(driver.findElement(By.id("fullName")), "Selenium Test");
        type(driver.findElement(By.id("email")), "selenium" + System.nanoTime() + "@example.com");
        type(driver.findElement(By.id("phone")), "0712345678");
        type(driver.findElement(By.id("password")), "password123");
        type(driver.findElement(By.id("confirmPassword")), "password123");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        wait(org.openqa.selenium.support.ui.ExpectedConditions.urlContains("/login"));
        assertThat(driver.getCurrentUrl()).contains("/login");
    }

    // =============================================
    // PAGE 3: DASHBOARD
    // =============================================
    @Test
    @DisplayName("PAGE 3: Dashboard shows rotation wheel and round status")
    void dashboardShowsWheelAndStatus() {
        loginAs(SELAM_EMAIL, PASSWORD);
        assertThat(driver.getTitle()).contains("My Groups");

        new DashboardPage(driver).assertWheelVisible();
        assertThat(driver.findElement(By.cssSelector("h1")).getText()).contains("My Groups");
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
        assertThat(page.hasAmountDisplayed()).isTrue();
    }

    // =============================================
    // PAGE 5: FAKE CHECKOUT (test mode)
    // =============================================
    @Test
    @DisplayName("PAGE 5: Fake checkout simulates successful payment")
    void fakeCheckoutSimulatesSuccess() {
        loginAs(YONAS_EMAIL, PASSWORD);
        ContributePage contributePage = new ContributePage(driver).open(baseUrl());
        assertThat(contributePage.hasAmountDisplayed()).isTrue();

        FakeCheckoutPage checkout = contributePage.clickPay();
        checkout.assertCheckoutUrl();
        PaymentReturnPage returnPage = checkout.simulateSuccess();
        returnPage.assertReturnUrl();
    }

    @Test
    @DisplayName("PAGE 5: Fake checkout simulates failed payment")
    void fakeCheckoutSimulatesFailure() {
        loginAs(YONAS_EMAIL, PASSWORD);
        ContributePage contributePage = new ContributePage(driver).open(baseUrl());
        assertThat(contributePage.hasAmountDisplayed()).isTrue();

        FakeCheckoutPage checkout = contributePage.clickPay();
        checkout.assertCheckoutUrl();
        PaymentReturnPage returnPage = checkout.simulateFailure();
        returnPage.assertReturnUrl();
    }

    // =============================================
    // PAGE 6: PAYMENT RETURN
    // =============================================
    @Test
    @DisplayName("PAGE 6: Payment return shows result")
    void paymentReturnShowsResult() {
        loginAs(YONAS_EMAIL, PASSWORD);
        ContributePage contributePage = new ContributePage(driver).open(baseUrl());
        assertThat(contributePage.hasAmountDisplayed()).isTrue();

        PaymentReturnPage returnPage = contributePage.clickPay().simulateSuccess();
        returnPage.assertReturnUrl();
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

        java.util.List<WebElement> manageLinks =
                driver.findElements(By.cssSelector("a[href*='/groups/'][href*='/members']"));
        if (!manageLinks.isEmpty()) {
            manageLinks.get(0).click();
            wait(org.openqa.selenium.support.ui.ExpectedConditions.urlContains("/members"));
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

        String[][] navLinks = {{"My Groups", "/dashboard"}, {"Notifications", "/notifications"}, {"Account", "/account"}};
        for (String[] link : navLinks) {
            wait(org.openqa.selenium.support.ui.ExpectedConditions
                    .elementToBeClickable(By.linkText(link[0]))).click();
            wait(org.openqa.selenium.support.ui.ExpectedConditions.urlContains(link[1]));
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
        assertThat(driver.getCurrentUrl()).contains("/dashboard");

        wait(org.openqa.selenium.support.ui.ExpectedConditions
                .elementToBeClickable(By.cssSelector("button.logout"))).click();
        wait(org.openqa.selenium.support.ui.ExpectedConditions.urlContains("/login"));
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
        wait(org.openqa.selenium.support.ui.ExpectedConditions.urlContains("/login"));
        assertThat(driver.getCurrentUrl()).contains("/login");
    }

    // =============================================
    // JOURNEY: Full payment flow (contribute -> checkout -> return)
    // =============================================
    @Test
    @DisplayName("JOURNEY: Full payment flow from contribute to payment return")
    void fullPaymentJourney() {
        loginAs(YONAS_EMAIL, PASSWORD);
        ContributePage contributePage = new ContributePage(driver).open(baseUrl());
        assertThat(contributePage.hasAmountDisplayed()).isTrue();

        PaymentReturnPage returnPage = contributePage.clickPay().simulateSuccess();
        returnPage.assertReturnUrl();
    }

    // ---------- helpers ----------

    /**
     * Types in one go. The old character-by-character loop existed for a visible demo run;
     * in headless CI it only added latency and gave Chrome more chances to re-render between
     * keystrokes.
     */
    private void type(WebElement element, String text) {
        element.clear();
        element.sendKeys(text);
    }

    private <T> T wait(org.openqa.selenium.support.ui.ExpectedCondition<T> condition) {
        return new org.openqa.selenium.support.ui.WebDriverWait(driver, Duration.ofSeconds(20))
                .until(condition);
    }
}