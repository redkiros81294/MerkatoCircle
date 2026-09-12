package com.merkatocircle.iqub.selenium.pages;

import org.openqa.selenium.WebDriver;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Page Object for the Round Detail page (/rounds/{id}).
 */
public class RoundDetailPage extends BasePage {

    public RoundDetailPage(WebDriver driver) {
        super(driver);
    }

    public void assertRoundDetailUrl() {
        assertThat(driver.getCurrentUrl()).matches(".*/rounds/\\d+");
    }
}
