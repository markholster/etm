package com.jecstar.etm.gui;

import com.consol.citrus.TestCaseMetaInfo;
import com.consol.citrus.dsl.runner.TestRunner;
import com.consol.citrus.selenium.endpoint.SeleniumBrowser;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Date;

/**
 * Abstract superclass for all Citrus Selenium test cases.
 */
public abstract class AbstractCitrusSeleniumTest {

    private final String etmUrl = "http://127.0.0.1:8080";
    protected final String username = "it-tester";
    private final String password = "Welcome123";

    /**
     * Gives the url Enterprise Telemetry Monitor is running on.
     *
     * @return The Enterprise Telemetry Monitor url.
     */
    protected String getEtmUrl() {
        return this.etmUrl;
    }

    /**
     * Set the metadata of the test on the <code>TestRunnen</code>.
     *
     * @param runner        The Citrus <code>TestRunner</code>.
     * @param author        The author of the test.
     * @param description   A description of the test.
     * @param creationYear  The year the test was created.
     * @param creationMonth The month the test was created.
     * @param creationDay   The day the test was created.
     */
    protected void setTestMetadata(TestRunner runner, String author, String description, int creationYear, Month creationMonth, int creationDay) {
        runner.author(author);
        runner.description(description);
        runner.status(TestCaseMetaInfo.Status.FINAL);
        runner.creationDate(
                Date.from(LocalDate.of(creationYear, creationMonth, creationDay)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                )
        );
    }

    /**
     * Login into ETM.
     *
     * @param runner  The Citrus <code>TestRunner</code>.
     * @param browser The Citrus <code>SeleniumBrowser</code>.
     */
    protected void login(TestRunner runner, SeleniumBrowser browser) {
        login(runner, browser, this.username, this.password);
    }

    /**
     * Login into ETM.
     *
     * @param runner   The Citrus <code>TestRunner</code>.
     * @param browser  The Citrus <code>SeleniumBrowser</code>.
     * @param username The username of the user to login.
     * @param password The password for the given username.
     */
    protected void login(TestRunner runner, SeleniumBrowser browser, String username, String password) {
        runner.selenium(action -> action.browser(browser).start());
        runner.selenium(action -> action.navigate(getEtmUrl() + "/gui/"));
        runner.selenium(action -> action.setInput(username).element(By.id("j_username")));
        runner.selenium(action -> action.setInput(password).element(By.id("j_password")));
        runner.selenium(action -> action.click().element(By.className("btn")));
        runner.selenium(action -> action.waitUntil().visible().element(By.id("event-count")));
    }

    /**
     * Wait for all ajax calls to complete.
     *
     * @param runner The Citrus <code>TestRunner</code>.
     */
    protected void waitForAjaxToComplete(TestRunner runner) {
        runner.run(new WaitForAjaxToCompleteAction());
    }

    /**
     * Wait until a bootstrap modal is shown.
     *
     * @param runner     The Citrus <code>TestRunner</code>.
     * @param modalTitle The modal title.
     */
    protected void waitForModalToShow(TestRunner runner, String modalTitle) {
        runner.selenium(action -> action.waitUntil().visible().element(By.xpath("//*[@class='modal-title' and text()='" + modalTitle + "']")));
    }

    /**
     * Wait until a bootstrap modal is hidden.
     *
     * @param runner     The Citrus <code>TestRunner</code>.
     * @param modalTitle The modal title.
     */
    protected void waitForModalToHide(TestRunner runner, String modalTitle) {
        runner.selenium(action -> action.waitUntil().hidden().element(By.xpath("//*[@class='modal-title' and text()='" + modalTitle + "']")));
    }

    /**
     * Confirm a modal with a certain answer.
     *
     * @param runner     The Citrus <code>TestRunner</code>.
     * @param modalTitle The modal title.
     * @param answer     The answer for the modal.
     */
    protected void confirmModalWith(TestRunner runner, String modalTitle, String answer) {
        sleep(500);
        runner.selenium(action -> action.click().element(By.xpath("//*[@class='modal-title' and text()='" + modalTitle + "']/../../div[@class='modal-footer']/button[text()='" + answer + "']")));
    }

    /**
     * Wait for all notification popups to be hidden.
     *
     * @param browser The Citrus <code>SeleniumBrowser</code>.
     */
    protected void waitForNotificationsToHide(SeleniumBrowser browser) {
        new WebDriverWait(browser.getWebDriver(), 10).until(f -> f.findElements(By.xpath("//div[@data-notify='container']")).isEmpty());
    }

    /**
     * Wait until an element will be present/added on the page.
     *
     * @param browser The Citrus <code>SeleniumBrowser</code>.
     * @param by      The element selector.
     */
    protected void waitForExistence(SeleniumBrowser browser, By by) {
        new WebDriverWait(browser.getWebDriver(), 5).until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    /**
     * Wait until an element will clickable.
     *
     * @param browser The Citrus <code>SeleniumBrowser</code>.
     * @param by      The element selector.
     */
    protected void waitForClickable(SeleniumBrowser browser, By by) {
        new WebDriverWait(browser.getWebDriver(), 5).until(ExpectedConditions.elementToBeClickable(by));
    }

    /**
     * Method removes an item from a page. The item will be selected and the remove button will be pushed. Finally the removal will be confirmed.
     *
     * @param runner   The Citrus <code>TestRunner</code>.
     * @param browser  The Citrus <code>SeleniumBrowser</code>.
     * @param itemType The item type that needs to be selected and removed.
     * @param itemId   The item id that needs to be selected and removed.
     */
    protected void selectAndRemoveItem(TestRunner runner, SeleniumBrowser browser, String itemType, String itemId) {
        Select itemSelect = new Select(browser.getWebDriver().findElement(By.id("sel-" + itemType)));
        if (itemSelect.getOptions().stream().anyMatch(p -> itemId.equals(p.getAttribute("value")))) {
            sleepWhenChrome(browser, 500);
            runner.selenium(action -> action.select(itemId).element(By.id("sel-" + itemType)));
            sleepWhenChrome(browser, 500);
            runner.selenium(action -> action.click().element(By.id("btn-confirm-remove-" + itemType)));
            waitForModalToShow(runner, "Confirm removal");
            confirmModalWith(runner, "Confirm removal", "Yes");
            waitForModalToHide(runner, "Confirm removal");
            waitForAjaxToComplete(runner);
        }
    }

    /**
     * Sleep for some time.
     *
     * @param sleepInMilliseconds The number of milliseconds to sleep.
     */
    protected void sleep(long sleepInMilliseconds) {
        try {
            Thread.sleep(sleepInMilliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    /**
     * Sleep when the browser is a Chrome browser.
     *
     * @param browser             The Citrus <code>SeleniumBrowser</code>.
     * @param sleepInMilliseconds The number of milliseconds to sleep.
     */
    protected void sleepWhenChrome(SeleniumBrowser browser, long sleepInMilliseconds) {
        if (browser.getWebDriver() instanceof ChromeDriver) {
            sleep(sleepInMilliseconds);
        }
    }

    /**
     * Send an event to Enterprise Telemetry Monitor.
     *
     * @param type The event type.
     * @param data The data that belongs to the event type.
     * @param apiKey The api key to use for authentication on the rest processor.
     * @return <code>true</code> when the event is acknowledges, <code>false</code> otherwise.
     * @throws IOException If the connection to Enterprise Telemetry Monitor fails for some reason.
     */
    protected boolean sendEventToEtm(String type, String data, String apiKey) throws IOException {
        HttpURLConnection con = null;
        DataOutputStream stream = null;
        BufferedReader in = null;
        try {
            var url = new URL(getEtmUrl() + "/rest/processor/event/");
            con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(1000);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            con.setRequestProperty("apikey", apiKey);
            con.setDoOutput(true);
            stream = new DataOutputStream(con.getOutputStream());
            stream.write(("{\"type\": \"" + type + "\", \"data\": " + data + "}").getBytes(Charset.forName("utf-8")));
            stream.flush();
            stream.close();

            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            con.disconnect();
            return "{ \"status\": \"acknowledged\" }".equals(response.toString().trim());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
            if (con != null) {
                con.disconnect();
            }
        }
    }
}
