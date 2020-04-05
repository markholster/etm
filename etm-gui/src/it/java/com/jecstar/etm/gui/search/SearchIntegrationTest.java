/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.gui.search;

import com.consol.citrus.annotations.CitrusEndpoint;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.dsl.junit.jupiter.CitrusExtension;
import com.consol.citrus.dsl.runner.TestRunner;
import com.consol.citrus.selenium.endpoint.SeleniumBrowser;
import com.jecstar.etm.domain.*;
import com.jecstar.etm.domain.builder.*;
import com.jecstar.etm.domain.writer.TelemetryEventWriter;
import com.jecstar.etm.domain.writer.json.HttpTelemetryEventWriterJsonImpl;
import com.jecstar.etm.domain.writer.json.LogTelemetryEventWriterJsonImpl;
import com.jecstar.etm.domain.writer.json.MessagingTelemetryEventWriterJsonImpl;
import com.jecstar.etm.domain.writer.json.SqlTelemetryEventWriterJsonImpl;
import com.jecstar.etm.integration.test.core.AbstractCitrusSeleniumTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.io.IOException;
import java.time.Instant;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CitrusExtension.class)
public class SearchIntegrationTest extends AbstractCitrusSeleniumTest {

    private final String searchPath = "/gui/search/index.html";
    private final TelemetryEventWriter<String, HttpTelemetryEvent> httpEventWriter = new HttpTelemetryEventWriterJsonImpl();
    private final TelemetryEventWriter<String, LogTelemetryEvent> logEventWriter = new LogTelemetryEventWriterJsonImpl();
    private final TelemetryEventWriter<String, MessagingTelemetryEvent> mqEventWriter = new MessagingTelemetryEventWriterJsonImpl();
    private final TelemetryEventWriter<String, SqlTelemetryEvent> sqlEventWriter = new SqlTelemetryEventWriterJsonImpl();

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
    public void testSearchInFirefox(@CitrusResource TestRunner runner) {
        setTestMetadata(runner, "Mark Holster", "Test the basic search capabilities in Firefox", 2018, Month.OCTOBER, 20);
        testSearch(runner, this.firefox);
    }

    @Test
    @CitrusTest
    public void testSearchInChrome(@CitrusResource TestRunner runner) {
        setTestMetadata(runner, "Mark Holster", "Test the basic search capabilities in Chrome", 2018, Month.OCTOBER, 20);
        testSearch(runner, this.chrome);
    }

    @Test
    @CitrusTest
    public void testSearchTemplatesInFirefox(@CitrusResource TestRunner runner) {
        setTestMetadata(runner, "Mark Holster", "Test the search template functionality in Firefox", 2018, Month.OCTOBER, 20);
        testSearchTemplates(runner, this.firefox);
    }

    @Test
    @CitrusTest
    public void testSearchTemplatesInChrome(@CitrusResource TestRunner runner) {
        setTestMetadata(runner, "Mark Holster", "Test the search template functionality in Chrome", 2018, Month.OCTOBER, 20);
        testSearchTemplates(runner, this.chrome);
    }

    @Test
    @CitrusTest
    public void testTransactionOverviewInFirefox(@CitrusResource TestRunner runner) throws IOException {
        setTestMetadata(runner, "Mark Holster", "Test the transaction overview functionality in Firefox", 2018, Month.OCTOBER, 20);
        testTransactionOverview(runner, this.firefox);
    }

    @Test
    @CitrusTest
    public void testTransactionOverviewInChrome(@CitrusResource TestRunner runner) throws IOException {
        setTestMetadata(runner, "Mark Holster", "Test the transaction overview functionality in Chrome", 2018, Month.OCTOBER, 20);
        testTransactionOverview(runner, this.chrome);
    }

    private void testSearch(TestRunner runner, SeleniumBrowser browser) {
        final String query = "endpoints.endpoint_handlers.application.name: \"Enterprise Telemetry Monitor\"";
        login(runner, browser);

        runner.selenium(action -> action.navigate(getEtmUrl() + searchPath));
        runner.selenium(action -> action.waitUntil().visible().element(By.id("search-container")));
        // Set the query input
        runner.selenium(action -> action.setInput(query).element(By.id("query-string")));
        waitForClickable(browser, By.id("btn-search"));
        runner.selenium(action -> action.click().element(By.id("btn-search")));
        waitForAjaxToComplete(runner);
        // Check the response table
        runner.selenium(action -> action.waitUntil().visible().element(By.id("result_card")));
        runner.selenium(action -> action.waitUntil().visible().element(By.id("search_result_table")));
        runner.selenium(action -> action.find().element(By.xpath("(//*[@id='search_result_table']/div[@class='row header-row']/div)[1]")).text("Timestamp"));
        runner.selenium(action -> action.find().element(By.xpath("(//*[@id='search_result_table']/div[@class='row header-row']/div)[2]")).text("Name"));
        // Check the search history
        runner.selenium(action -> action.find().element(By.xpath("//*[@id='list-search-history-links']/li[1]/a[1]")).attribute("title", query));
        // Sort the result table by name
        runner.selenium(action -> action.click().element(By.xpath("//*[@id='search_result_table']/div[@class='row header-row']/div[text()='Name']")));
        waitForAjaxToComplete(runner);
        runner.selenium(action -> action.find().element(By.xpath("//*[@id='search_result_table']/div[@class='row header-row']/div[@class='col font-weight-bold headerSortDesc' and text()='Name']")));
        // Add a columnt to the result table
        runner.selenium(action -> action.click().element(By.id("searchresult-dropdown-menu")));
        runner.selenium(action -> action.click().element(By.xpath("//*[@id='search-result-card']//a[@data-action='edit-result-table']")));
        waitForModalToShow(runner, "Table settings");
        runner.selenium(action -> action.click().element(By.id("link-add-result-row")));
        runner.selenium(action -> action.setInput("Log level").element(By.xpath("//*[@id='table-settings-columns']/*[4]/*[1]/*[1]")));
        runner.selenium(action -> action.setInput("log_level").element(By.xpath("//*[@id='table-settings-columns']/*[4]/*[2]/*[1]")));
        confirmModalWith(runner, "Table settings", "Apply");
        waitForModalToHide(runner, "Table settings");
        // Check if the new column is present
        waitForAjaxToComplete(runner);
        runner.selenium(action -> action.find().element(By.xpath("(//*[@id='search_result_table']/div[@class='row header-row']/div)[3]")).text("Log level"));
    }

    private void testSearchTemplates(TestRunner runner, SeleniumBrowser browser) {
        final String query = "This is a test";
        final String updatedQeury = "This is an updated test";
        final String templateName = "Integration test";
        login(runner, browser);
        runner.selenium(action -> action.navigate(getEtmUrl() + searchPath));
        runner.selenium(action -> action.waitUntil().visible().element(By.id("search-container")));
        waitForAjaxToComplete(runner);
        // First remove all existing templates
        removeSearchTemplates(runner, browser);
        // Check the state of some elements
        runner.selenium(action -> action.find().element(By.id("btn-search")).enabled(false));
        runner.selenium(action -> action.find().element(By.id("template-name")).enabled(false));
        runner.selenium(action -> action.find().element(By.id("btn-save-template")).enabled(false));
        // Now enter a query and check the state again
        runner.selenium(action -> action.setInput(query).element(By.id("query-string")));
        runner.selenium(action -> action.find().element(By.id("btn-search")).enabled(true));
        runner.selenium(action -> action.find().element(By.id("template-name")).enabled(true));
        runner.selenium(action -> action.find().element(By.id("btn-save-template")).enabled(false));
        // Now enter a template name as well. The save template buttons should be enabled as well.
        runner.selenium(action -> action.setInput(templateName).element(By.id("template-name")));
        runner.selenium(action -> action.find().element(By.id("btn-save-template")).enabled(true));
        // Now save the template and check if it is present in the template list
        runner.selenium(action -> action.click().element(By.id("btn-save-template")));
        waitForAjaxToComplete(runner);
        runner.selenium(action -> action.find().element(By.xpath("//*[@id='list-template-links']/li/a[1]")).text(templateName));
        // Finally test if it is possible to update the template.
        runner.selenium(action -> action.setInput(updatedQeury).element(By.id("query-string")));
        runner.selenium(action -> action.setInput(templateName).element(By.id("template-name")));
        waitForClickable(browser, By.id("btn-save-template"));
        runner.selenium(action -> action.click().element(By.id("btn-save-template")));
        // The overwrite modal should be shows
        waitForModalToShow(runner, "Template already exists");
        confirmModalWith(runner, "Template already exists", "Yes");
        waitForModalToHide(runner, "Template already exists");
        waitForNotificationsToHide(browser);
        // Now test if the updated template works.
        runner.selenium(action -> action.setInput("This text should be overwritten by the template").element(By.id("query-string")));
        runner.selenium(action -> action.click().element(By.xpath("//*[@id='list-template-links']/li/a[text()='" + templateName + "']")));
        runner.selenium(action -> action.find().element(By.id("query-string")).attribute("value", updatedQeury));
    }

    private void testTransactionOverview(TestRunner runner, SeleniumBrowser browser) throws IOException {
        login(runner, browser);
        var apiKeys = getApiKeys(runner, browser);
        var eventId = UUID.randomUUID().toString();
        addTransactionsToEtm(eventId, apiKeys);
        runner.selenium(action -> action.navigate(getEtmUrl() + searchPath));
        runner.selenium(action -> action.waitUntil().visible().element(By.id("search-container")));
        // Search for the given event id.
        runner.selenium(action -> action.setInput("_id: " + eventId).element(By.id("query-string")));
        // Wait for elasticsearch to make the event searchable
        waitForClickable(browser, By.id("btn-search"));
        while (browser.getWebDriver().findElements(By.id(eventId)).size() == 0) {
            runner.selenium(action -> action.click().element(By.id("btn-search")));
            waitForAjaxToComplete(runner);
        }
        // Select the item and click on the Endpoints tab
        runner.selenium(action -> action.click().element(By.id(eventId)));
        waitForAjaxToComplete(runner);
        runner.selenium(action -> action.click().element(By.xpath("//ul[@id='event-tabs']/li/a[text()='Endpoints']")));
        runner.selenium(action -> action.waitUntil().visible().element(By.id("endpoint-overview")));
        sleep(1000);
        // There should be 1 svg items
        assertEquals(1, browser.getWebDriver().findElement(By.id("endpoint-overview")).findElements(By.tagName("svg")).size());
        // Now select an event in the gui application
        List<WebElement> nodeElements = browser.getWebDriver().findElement(By.className("highcharts-mainGroup")).findElements(By.className("application"));
        // We expect 2 applications
        assertEquals(2, nodeElements.size());
        WebElement node = nodeElements.get(0);
        // Select the events within the application.
        nodeElements = node.findElements(By.className("etm-gen-drawing-message"));
        // We expect 4 applications
        assertEquals(4, nodeElements.size());
        node = nodeElements.get(0);
        new Actions(browser.getWebDriver()).moveToElement(node).click().perform();
        // The event is clicked, now the transaction should be visible
        waitForExistence(browser, By.id("transaction-detail-table"));
        // Wait for the table to be fully built.
        sleep(500);
        //  There should be 8 rows in the transaction detail table. A header and 7 events.
        assertEquals(8, browser.getWebDriver().findElement(By.id("transaction-detail-table")).findElements(By.tagName("tr")).size());
        // Now check the event chain tab
        runner.selenium(action -> action.click().element(By.xpath("//ul[@id='event-tabs']/li/a[text()='Chain times']")));
        runner.selenium(action -> action.waitUntil().visible().element(By.id("event-chain")));
        sleep(1000);
        nodeElements = browser.getWebDriver().findElement(By.id("event-chain")).findElements(By.cssSelector("g.highcharts-point"));
        assertEquals(4, nodeElements.size());
        node = nodeElements.get(1);
        new Actions(browser.getWebDriver()).moveToElement(node).click().perform();
        waitForExistence(browser, By.id("transaction-detail-table"));
        sleep(500);
        assertEquals(8, browser.getWebDriver().findElement(By.id("transaction-detail-table")).findElements(By.tagName("tr")).size());
    }

    private void removeSearchTemplates(TestRunner runner, SeleniumBrowser browser) {
        List<WebElement> removeLinks = browser.getWebDriver().findElement(By.id("list-template-links")).findElements(By.xpath("./li/a[contains(@class, 'fa-times')]"));
        for (WebElement removeLink : removeLinks) {
            removeLink.click();
            runner.selenium(action -> action.waitUntil().visible().element(By.id("btn-remove-template")));
            sleep(500);
            runner.selenium(action -> action.click().element(By.id("btn-remove-template")));
            runner.selenium(action -> action.waitUntil().hidden().element(By.xpath("//*[@class='modal-title' and text()='Confirm removal']")));
        }
    }

    private void addTransactionsToEtm(String eventId, List<String> apiKeys) throws IOException {
        final EndpointHandlerBuilder guiEndpointHandler = new EndpointHandlerBuilder()
                .setTransactionId(UUID.randomUUID().toString())
                .setApplication(new ApplicationBuilder()
                        .setName("Gui application")
                        .setVersion("1.0.0")
                );
        final EndpointHandlerBuilder backendEndpointHandler = new EndpointHandlerBuilder()
                .setTransactionId(UUID.randomUUID().toString())
                .setApplication(new ApplicationBuilder()
                        .setName("My Backend")
                        .setInstance("Instance 3")
                        .setVersion("2.1.0_beta3")
                );

        // A user requests the shopping card page from our public http site.
        Instant timestamp = Instant.now();
        guiEndpointHandler.setHandlingTime(timestamp);
        guiEndpointHandler.setSequenceNumber(-1);
        guiEndpointHandler
                .addMetadata("User-Agent", "Mozilla/5.0 (X11; Fedora; Linux x86_64; rv:46.0) Gecko/20100101 Firefox/46.0")
                .addMetadata("Pragma", "no-cache");

        assertTrue(sendEventToEtm("http", null, this.httpEventWriter.write(new HttpTelemetryEventBuilder()
                .setId(eventId)
                .setPayload("GET http://www.my-company.com/shopping-card.html")
                .setName("GetShoppingCard")
                .setPayloadFormat(PayloadFormat.TEXT)
                .setHttpEventType(HttpTelemetryEvent.HttpEventType.GET)
                .setExpiry(timestamp.plusSeconds(30))
                .addMetadata("BusinessProcess", "User views shopping card")
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("http://www.my-company.com/shopping-card.html")
                        .addEndpointHandler(new EndpointHandlerBuilder().setHandlingTime(timestamp).setType(EndpointHandler.EndpointHandlerType.WRITER))
                ).addOrMergeEndpoint(new EndpointBuilder()
                        .setName("http://instserv0001.my.corp/shopping-card.html")
                        .addEndpointHandler(guiEndpointHandler.setType(EndpointHandler.EndpointHandlerType.READER))
                )
                .build()), apiKeys));

        // Add some logging generated by our gui app.
        guiEndpointHandler.setSequenceNumber(1);
        assertTrue(sendEventToEtm("log", null, this.logEventWriter.write(new LogTelemetryEventBuilder()
                .setId(UUID.randomUUID().toString())
                .setPayload("Found user")
                .setPayloadFormat(PayloadFormat.TEXT)
                .setLogLevel("DEBUG")
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("com.my-company.gui.Httphandler.handleRequest(Httphandler.java:60")
                        .addEndpointHandler(guiEndpointHandler.setType(EndpointHandler.EndpointHandlerType.WRITER))
                )
                .build()), apiKeys));

        guiEndpointHandler.setSequenceNumber(0);
        assertTrue(sendEventToEtm("log", null, this.logEventWriter.write(new LogTelemetryEventBuilder()
                .setId(UUID.randomUUID().toString())
                .setPayload("User is requesting his/her shopping card.")
                .setPayloadFormat(PayloadFormat.TEXT)
                .setLogLevel("DEBUG")
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("com.my-company.gui.Httphandler.handleRequest(Httphandler.java:59)")
                        .addEndpointHandler(guiEndpointHandler.setType(EndpointHandler.EndpointHandlerType.WRITER))
                )
                .build()), apiKeys));


        timestamp = timestamp.plus(8, ChronoUnit.MILLIS);
        guiEndpointHandler.setHandlingTime(timestamp);
        guiEndpointHandler.setSequenceNumber(3);
        assertTrue(sendEventToEtm("log", null, this.logEventWriter.write(new LogTelemetryEventBuilder()
                .setId(UUID.randomUUID().toString())
                .setPayload("Requesting shoppping card over MQ.")
                .setPayloadFormat(PayloadFormat.TEXT)
                .setLogLevel("DEBUG")
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("com.my-company.gui.MqRequestor.requestShoppingCar(MqRequestor.java:352)")
                        .addEndpointHandler(guiEndpointHandler.setType(EndpointHandler.EndpointHandlerType.WRITER))
                )
                .build()), apiKeys));

        // Now let the gui app send an MQ request to the backend app.
        timestamp = timestamp.plus(1, ChronoUnit.MILLIS);
        guiEndpointHandler.setHandlingTime(timestamp);
        guiEndpointHandler.setSequenceNumber(null);
        timestamp = timestamp.plus(3, ChronoUnit.MILLIS);
        backendEndpointHandler.setHandlingTime(timestamp);
        String mqMessageId = UUID.randomUUID().toString();
        assertTrue(sendEventToEtm("messaging", null, this.mqEventWriter.write(new MessagingTelemetryEventBuilder()
                .setId(mqMessageId)
                .setPayload("<shoppingcard_request><customer_id>543214</customer_id></shoppingcard_request>")
                .setPayloadFormat(PayloadFormat.XML)
                .setMessagingEventType(MessagingTelemetryEvent.MessagingEventType.REQUEST)
                .setName("ShoppingCardRequest")
                .setExpiry(timestamp.plusSeconds(30))
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("wmq://BACKEND.QUEUE.1")
                        .addEndpointHandler(guiEndpointHandler.setType(EndpointHandler.EndpointHandlerType.WRITER))
                        .addEndpointHandler(backendEndpointHandler.setType(EndpointHandler.EndpointHandlerType.READER))
                )
                .build()), apiKeys));

        // Add some backend logging
        timestamp = timestamp.plus(7, ChronoUnit.MILLIS);
        backendEndpointHandler.setHandlingTime(timestamp);
        assertTrue(sendEventToEtm("log", null, this.logEventWriter.write(new LogTelemetryEventBuilder()
                .setId(UUID.randomUUID().toString())
                .setPayload("Received shopping card request.")
                .setPayloadFormat(PayloadFormat.TEXT)
                .setLogLevel("DEBUG")
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("com.my-company.backend.MqHandler.handleRequest(MqHandler.java:103)")
                        .addEndpointHandler(backendEndpointHandler.setType(EndpointHandler.EndpointHandlerType.WRITER))
                )
                .build()), apiKeys));

        // Request the shopping card from the db.
        timestamp = timestamp.plus(10, ChronoUnit.MILLIS);
        backendEndpointHandler.setHandlingTime(timestamp);
        String sqlRequestId = UUID.randomUUID().toString();
        assertTrue(sendEventToEtm("sql", null, this.sqlEventWriter.write(new SqlTelemetryEventBuilder()
                .setPayload("select * from shoppingcard where customerId = ?")
                .setId(sqlRequestId)
                .setDbQueryEventType(SqlTelemetryEvent.SqlEventType.SELECT)
                .setPayloadFormat(PayloadFormat.SQL)
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("TAB_CUSTOMER")
                        .addEndpointHandler(backendEndpointHandler.setType(EndpointHandler.EndpointHandlerType.WRITER))
                )
                .build()), apiKeys));
        timestamp = timestamp.plus(275, ChronoUnit.MILLIS);
        backendEndpointHandler.setHandlingTime(timestamp);
        assertTrue(sendEventToEtm("sql", null, this.sqlEventWriter.write(new SqlTelemetryEventBuilder()
                .setId(UUID.randomUUID().toString())
                .setCorrelationId(sqlRequestId)
                .setPayload("found 10 results")
                .setDbQueryEventType(SqlTelemetryEvent.SqlEventType.RESULTSET)
                .setPayloadFormat(PayloadFormat.SQL)
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("TAB_CUSTOMER")
                        .addEndpointHandler(backendEndpointHandler.setType(EndpointHandler.EndpointHandlerType.READER))
                )
                .build()), apiKeys));

        // Send a MQ response back to the gui app.
        timestamp = timestamp.plus(30, ChronoUnit.MILLIS);
        backendEndpointHandler.setHandlingTime(timestamp);
        timestamp = timestamp.plus(4, ChronoUnit.MILLIS);
        guiEndpointHandler.setHandlingTime(timestamp);
        assertTrue(sendEventToEtm("messaging", null, this.mqEventWriter.write(new MessagingTelemetryEventBuilder()
                .setId(UUID.randomUUID().toString())
                .setCorrelationId(mqMessageId)
                .setPayload("<shoppingcard_response><customer_id>543214</customer_id></shoppingcard_response>")
                .setPayloadFormat(PayloadFormat.XML)
                .setName("ShoppingCardResponse")
                .setMessagingEventType(MessagingTelemetryEvent.MessagingEventType.RESPONSE)
                .setExpiry(timestamp.plusSeconds(30))
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("wmq://FRONTEND.QUEUE.1")
                        .addEndpointHandler(backendEndpointHandler.setType(EndpointHandler.EndpointHandlerType.WRITER))
                        .addEndpointHandler(guiEndpointHandler.setType(EndpointHandler.EndpointHandlerType.READER))
                )
                .build()), apiKeys));

        // And finally let the gui app return the html page.
        timestamp = timestamp.plus(32, ChronoUnit.MILLIS);
        guiEndpointHandler.setHandlingTime(timestamp);
        assertTrue(sendEventToEtm("http", null, this.httpEventWriter.write(new HttpTelemetryEventBuilder()
                .setId(UUID.randomUUID().toString())
                .setCorrelationId(eventId)
                .setPayload("<html><body><p>We found 2 items in your shopping card</p></body></html>")
                .setPayloadFormat(PayloadFormat.HTML)
                .setName("ReturnShoppingCard")
                .setHttpEventType(HttpTelemetryEvent.HttpEventType.RESPONSE)
                .setStatusCode(200)
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("http://www.my-company.com/shopping-card.html")
                        .addEndpointHandler(new EndpointHandlerBuilder().setHandlingTime(timestamp).setType(EndpointHandler.EndpointHandlerType.READER))
                ).addOrMergeEndpoint(new EndpointBuilder()
                        .setName("http://instserv0001.my.corp/shopping-card.html")
                        .addEndpointHandler(guiEndpointHandler.setType(EndpointHandler.EndpointHandlerType.WRITER))
                )
                .build()), apiKeys));
        // Let elasticsearch index the events.
        sleep(3000);
    }
}
