package com.jecstar.etm.gui.settings;

import com.consol.citrus.annotations.CitrusEndpoint;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.dsl.junit.jupiter.CitrusExtension;
import com.consol.citrus.dsl.runner.TestRunner;
import com.consol.citrus.selenium.endpoint.SeleniumBrowser;
import com.jecstar.etm.integration.test.core.AbstractCitrusSeleniumTest;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.principal.SecurityRoles;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CitrusExtension.class)
public class UsersIntegrationTest extends AbstractCitrusSeleniumTest {

    private final String usersPath = "/gui/settings/users.html";

    @CitrusEndpoint(name = "firefox")
    private SeleniumBrowser firefox;

    @CitrusEndpoint(name = "chrome")
    private SeleniumBrowser chrome;

    @AfterAll
    private void afterAll() {
        this.firefox.stop();
        this.chrome.stop();
    }

    @Test
    @CitrusTest
    public void testRemoveLatestAdminAccounInFirefox(@CitrusResource TestRunner runner) {
        setTestMetadata(runner, "Mark Holster", "Test if removing the latest admin account fails in Firefox", 2018, Month.OCTOBER, 20);
        testRemoveLatestAdminAccount(runner, this.firefox);
    }

    @Test
    @CitrusTest
    public void testRemoveLatestAdminAccounInChrome(@CitrusResource TestRunner runner) {
        setTestMetadata(runner, "Mark Holster", "Test if removing the latest admin account fails in Chrome", 2018, Month.OCTOBER, 20);
        testRemoveLatestAdminAccount(runner, this.chrome);
    }

    private void testRemoveLatestAdminAccount(TestRunner runner, SeleniumBrowser browser) {
        login(runner, browser);
        runner.selenium(action -> action.navigate(getEtmUrl() + usersPath));
        runner.selenium(action -> action.waitUntil().visible().element(By.id("users_box")));
        waitForAjaxToComplete(runner);
        //Give all users the read access except the user we use in this integration test.
        Select userSelect = new Select(browser.getWebDriver().findElement(By.id("sel-user")));
        List<WebElement> options = userSelect.getOptions();
        for (WebElement user : options) {
            if (user.getAttribute("value") == null || "".equals(user.getAttribute("value").trim())) {
                // Skip the empty option
                continue;
            }
            if (!username.equals(user.getAttribute("value"))) {
                waitForClickable(browser, By.id("sel-user"));
                userSelect.selectByValue(user.getAttribute("value"));
                sleepWhenChrome(browser, 500);

                Select userSettingsAcl = new Select(browser.getWebDriver().findElement(By.id("sel-user-settings-acl")));
                if ("user_settings_read_write".equals(userSettingsAcl.getFirstSelectedOption().getAttribute("value"))) {
                    userSettingsAcl.selectByValue(SecurityRoles.USER_SETTINGS_READ);
                    sleepWhenChrome(browser, 500);
                    runner.selenium(action -> action.click().element(By.id("btn-confirm-save-user")));
                    waitForModalToShow(runner, "User already exists");
                    confirmModalWith(runner, "User already exists", "Yes");
                    waitForModalToHide(runner, "User already exists");
                    waitForAjaxToComplete(runner);
                    waitForNotificationsToHide(browser);
                }
            }
        }
        // Now select the user we use for this test.
        runner.selenium(action -> action.select(username).element(By.id("sel-user")));
        // And give it user settings read rights.
        testSaveAndDeleteWithRole(runner, browser, SecurityRoles.USER_SETTINGS_READ);
        testSaveAndDeleteWithRole(runner, browser, "none");
        // Restore the admin user
        runner.selenium(action -> action.select("admin").element(By.id("sel-user")));
        runner.selenium(action -> action.select(SecurityRoles.USER_SETTINGS_READ_WRITE).element(By.id("sel-user-settings-acl")));
        runner.selenium(action -> action.click().element(By.id("btn-confirm-save-user")));
        waitForModalToShow(runner, "User already exists");
        confirmModalWith(runner, "User already exists", "Yes");
        waitForModalToHide(runner, "User already exists");
        waitForAjaxToComplete(runner);
    }

    private void testSaveAndDeleteWithRole(TestRunner runner, SeleniumBrowser browser, String userSettingsAcl) {
        runner.selenium(action -> action.select(userSettingsAcl).element(By.id("sel-user-settings-acl")));
        // Now try to save the user. This should fail.
        runner.selenium(action -> action.click().element(By.id("btn-confirm-save-user")));
        waitForModalToShow(runner, "User already exists");
        confirmModalWith(runner, "User already exists", "Yes");
        waitForModalToHide(runner, "User already exists");
        waitForAjaxToComplete(runner);
        // An error box should be shown.
        String errorText = browser.getWebDriver().findElement(By.xpath("//span[@data-notify='message']")).getText();
        assertTrue(errorText.contains("" + EtmException.NO_MORE_USER_ADMINS_LEFT), errorText);
        waitForNotificationsToHide(browser);
        // A try to remove the user should also fail
        runner.selenium(action -> action.click().element(By.id("btn-confirm-remove-user")));
        waitForModalToShow(runner, "Confirm removal");
        confirmModalWith(runner, "Confirm removal", "Yes");
        waitForModalToHide(runner, "Confirm removal");
        waitForAjaxToComplete(runner);
        // An error box should be shown.
        errorText = browser.getWebDriver().findElement(By.xpath("//span[@data-notify='message']")).getText();
        assertTrue(errorText.contains("" + EtmException.NO_MORE_USER_ADMINS_LEFT), errorText);
        waitForNotificationsToHide(browser);
    }


}
