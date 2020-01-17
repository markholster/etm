package com.jecstar.etm.gui.settings;

import com.consol.citrus.annotations.CitrusEndpoint;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.dsl.junit.jupiter.CitrusExtension;
import com.consol.citrus.dsl.runner.TestRunner;
import com.consol.citrus.selenium.endpoint.SeleniumBrowser;
import com.jecstar.etm.domain.HttpTelemetryEvent;
import com.jecstar.etm.domain.PayloadFormat;
import com.jecstar.etm.domain.builder.HttpTelemetryEventBuilder;
import com.jecstar.etm.domain.writer.TelemetryEventWriter;
import com.jecstar.etm.domain.writer.json.HttpTelemetryEventWriterJsonImpl;
import com.jecstar.etm.integration.test.core.AbstractCitrusSeleniumTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;

import java.io.IOException;
import java.time.Month;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CitrusExtension.class)
public class ParsersAndImportProfilesIntegrationTest extends AbstractCitrusSeleniumTest {

    private final String parsersPath = "/gui/settings/parsers.html";
    private final String importProfilesPath = "/gui/settings/import_profiles.html";
    private final String searchPath = "/gui/search/index.html";
    private final TelemetryEventWriter<String, HttpTelemetryEvent> httpEventWriter = new HttpTelemetryEventWriterJsonImpl();

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
    public void testImportProfilesInFirefox(@CitrusResource TestRunner runner) throws IOException {
        setTestMetadata(runner, "Mark Holster", "Test the import profiles in Firefox", 2019, Month.MAY, 31);
        testImportProfiles(runner, this.firefox);
    }

    @Test
    @CitrusTest
    public void testImportProfilesInChrome(@CitrusResource TestRunner runner) throws IOException {
        setTestMetadata(runner, "Mark Holster", "Test the import profiles in Chrome", 2019, Month.MAY, 31);
        testImportProfiles(runner, this.chrome);
    }

    private void testImportProfiles(TestRunner runner, SeleniumBrowser browser) throws IOException {
        final var parserName = "Integration Test Parser";
        final var importProfileName = "Integration Test Import Profile";

        login(runner, browser);
        var apiKey = getApiKeys(runner, browser);
        runner.selenium(action -> action.navigate(getEtmUrl() + parsersPath));
        runner.selenium(action -> action.waitUntil().visible().element(By.id("parsers_box")));
        waitForAjaxToComplete(runner);

        // Remove the parser if present
        selectAndRemoveItem(runner, browser, "parser", parserName);

        runner.selenium(action -> action.setInput(parserName).element(By.id("input-parser-name")));
        runner.selenium(action -> action.select("fixed_value").element(By.id("sel-parser-type")));
        runner.selenium(action -> action.waitUntil().visible().element(By.id("input-fixed-value")));
        runner.selenium(action -> action.setInput("Value from import profile").element(By.id("input-fixed-value")));
        waitForClickable(browser, By.id("btn-confirm-save-parser"));
        runner.selenium(action -> action.click().element(By.id("btn-confirm-save-parser")));
        waitForAjaxToComplete(runner);

        // Give elasticsearch some time to make the parser loadable in the next page.
        sleep(3000);
        runner.selenium(action -> action.navigate(getEtmUrl() + importProfilesPath));
        runner.selenium(action -> action.waitUntil().visible().element(By.id("import-profiles_box")));
        waitForAjaxToComplete(runner);

        // Remove the import profile if present
        if (selectAndRemoveItem(runner, browser, "import-profile", importProfileName)) {
            // No option to select an option without a value so we reload the page.
            runner.selenium(action -> action.navigate(getEtmUrl() + importProfilesPath));
            runner.selenium(action -> action.waitUntil().visible().element(By.id("import-profiles_box")));
            waitForAjaxToComplete(runner);
        }

        runner.selenium(action -> action.setInput(importProfileName).element(By.id("input-import-profile-name")));
        runner.selenium(action -> action.select("true").element(By.id("sel-detect-payload-format")));
        runner.selenium(action -> action.click().element(By.id("link-add-extraction-field")));
        runner.selenium(action -> action.select("extracted_data.").element(By.xpath("//select[@class='form-control custom-select etm-parser-field']")));
        runner.selenium(action -> action.setInput("it-test").element(By.xpath("//input[@class='form-control etm-collection-key']")));
        runner.selenium(action -> action.select(parserName).element(By.xpath("//select[@class='form-control custom-select etm-expression-parser']")));
        waitForClickable(browser, By.id("btn-confirm-save-import-profile"));
        runner.selenium(action -> action.click().element(By.id("btn-confirm-save-import-profile")));
        waitForAjaxToComplete(runner);

        // Give elasticsearch some time to make the import profile available in the processor.
        sleep(3000);
        final var eventId1 = UUID.randomUUID().toString();
        final var eventId2 = UUID.randomUUID().toString();
        assertTrue(sendEventToEtm("http", null, this.httpEventWriter.write(new HttpTelemetryEventBuilder()
                .setId(eventId1)
                .setPayload("GET http://www.my-company.com/import_profile.html")
                .setName("GetImportProfile")
                .setPayloadFormat(PayloadFormat.TEXT)
                .setHttpEventType(HttpTelemetryEvent.HttpEventType.GET)
                .build()), apiKey));

        assertTrue(sendEventToEtm("http", importProfileName, this.httpEventWriter.write(new HttpTelemetryEventBuilder()
                .setId(eventId2)
                .setPayload("GET http://www.my-company.com/import_profile.html")
                .setName("GetImportProfile")
                .setPayloadFormat(PayloadFormat.TEXT)
                .setHttpEventType(HttpTelemetryEvent.HttpEventType.GET)
                .build()), apiKey));

        // Let elasticsearch index the events.
        sleep(3000);

        runner.selenium(action -> action.navigate(getEtmUrl() + searchPath));
        runner.selenium(action -> action.waitUntil().visible().element(By.id("search-container")));
        // Search for the given event id.
        runner.selenium(action -> action.setInput("_id: " + eventId1).element(By.id("query-string")));
        // Wait for elasticsearch to make the event searchable
        while (browser.getWebDriver().findElements(By.id(eventId1)).size() == 0) {
            runner.selenium(action -> action.click().element(By.id("btn-search")));
            waitForAjaxToComplete(runner);
        }
        // Select the event
        runner.selenium(action -> action.click().element(By.id(eventId1)));
        waitForAjaxToComplete(runner);
        // The first event is processed without an attached import profile
        assertSame(0, browser.getWebDriver().findElements(By.xpath("//a[text()='Extracted data']")).size());

        // Now find the second event.
        runner.selenium(action -> action.navigate(getEtmUrl() + searchPath));
        runner.selenium(action -> action.waitUntil().visible().element(By.id("search-container")));
        // Search for the given event id.
        runner.selenium(action -> action.setInput("_id: " + eventId2).element(By.id("query-string")));
        // Wait for elasticsearch to make the event searchable
        while (browser.getWebDriver().findElements(By.id(eventId2)).size() == 0) {
            runner.selenium(action -> action.click().element(By.id("btn-search")));
            waitForAjaxToComplete(runner);
        }
        // Select the event
        runner.selenium(action -> action.click().element(By.id(eventId2)));
        waitForAjaxToComplete(runner);
        // The second event is processed with an attached import profile
        assertSame(1, browser.getWebDriver().findElements(By.xpath("//a[text()='Extracted data']")).size());
    }
}
