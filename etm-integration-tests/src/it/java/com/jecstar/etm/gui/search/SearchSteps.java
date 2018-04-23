package com.jecstar.etm.gui.search;

import com.jecstar.etm.domain.*;
import com.jecstar.etm.domain.builder.*;
import com.jecstar.etm.domain.writer.TelemetryEventWriter;
import com.jecstar.etm.domain.writer.json.HttpTelemetryEventWriterJsonImpl;
import com.jecstar.etm.domain.writer.json.LogTelemetryEventWriterJsonImpl;
import com.jecstar.etm.domain.writer.json.MessagingTelemetryEventWriterJsonImpl;
import com.jecstar.etm.domain.writer.json.SqlTelemetryEventWriterJsonImpl;
import com.jecstar.etm.gui.AbstractSteps;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


@SuppressWarnings("unused")
public class SearchSteps extends AbstractSteps {

    private final TelemetryEventWriter<String, HttpTelemetryEvent> httpEventWriter = new HttpTelemetryEventWriterJsonImpl();
    private final TelemetryEventWriter<String, LogTelemetryEvent> logEventWriter = new LogTelemetryEventWriterJsonImpl();
    private final TelemetryEventWriter<String, MessagingTelemetryEvent> mqEventWriter = new MessagingTelemetryEventWriterJsonImpl();
    private final TelemetryEventWriter<String, SqlTelemetryEvent> sqlEventWriter = new SqlTelemetryEventWriterJsonImpl();

    public SearchSteps() {
        // Execute a query with a certain type.
        When("The user searches for (.*) of types (.*)", (String query, String types) -> {
            selectQueryTypes(types);
            findById("query-string-from").clear();
            findById("query-string-till").clear();
            setTextToElement(findById("query-string"), query);
            findById("btn-search").click();
            waitForShow("search_result_table");
        });

        // Check if the search page is visible
        Then("The search page should be visible", () ->
                assertNotNull(findById("search-container"))
        );

        And("The search result table should be visible and contain (\\d+) columns", (Integer nrOfColumns) -> {
            WebElement searchResultTable = findById("search_result_table");
            assertNotNull(searchResultTable);
            assertEquals(nrOfColumns.intValue(), searchResultTable.findElements(By.xpath("./thead/tr/th")).size());
        });

        // Check the first entry of the search history widget for the given query.
        And("The search history should contain (.*)", (String query) ->
                waitFor(d -> {
                    WebElement historyLink = d.findElement(By.id("list-search-history-links")).findElement(By.xpath("./*[1]/*[1]"));
                    return historyLink != null && historyLink.getAttribute("title").equals(query);
                })
        );

        // Sort the search results by a given column.
        When("The user sorts the search results by (.*)", (String sortColumn) -> {
            WebElement nameColumn = findById("search_result_table").findElement(By.xpath("./thead/tr/th[text()='" + sortColumn + "']"));
            assertNotNull(nameColumn);
            nameColumn.click();
        });

        // Check if the search result table is sorted by the given column.
        Then("The result table should be sorted by (.*)", (String columnName) ->
                waitFor(d ->
                        d.findElement(By.id("search_result_table")).findElement(By.xpath("./thead/tr/th[@class='headerSortDesc' and text()='" + columnName + "']")) != null
                )
        );

        // Click on the link that open the edit table settings modal screen
        When("The result table edit link is clicked", () -> findById("link-edit-table").click());

        When("The user adds a row with the name \"(.*)\" on the field \"(.*)\"", (String columnName, String columnField) -> {
            clickOnElement("link-add-result-row");
            // Select the fourth child. first child is the header, second is the timestamp column, third is the name column and the fourth is our new column.
            WebElement newRowElement = findById("table-settings-columns").findElement(By.xpath("./*[4]"));
            // Set the column name and field
            newRowElement.findElement(By.xpath("./*[1]/*[1]")).sendKeys(columnName);
            newRowElement.findElement(By.xpath("./*[2]/*[1]")).sendKeys(columnField);
            // Save the new layout.
            clickOnElement("btn-apply-table-settings");
            waitForModalToHide("Table settings");
        });

        When("The user enters the text \"(.*)\" in the query field", (String query) -> setTextToElement(findById("query-string"), query));
        When("The user enters the text \"(.*)\" in the template name field", (String query) -> setTextToElement(findById("template-name"), query));

        And("All templates are removed", () -> {
            // Wait for the page to fully load.
            try {
                waitFor(c -> findById("list-template-links").findElements(By.xpath("./*/a[contains(@class, 'fa-times')]")).size() > 0, 2);
            } catch (TimeoutException e) {
                // NO-OP
            }
            List<WebElement> removeLinks = findById("list-template-links").findElements(By.xpath("./*/a[contains(@class, 'fa-times')]"));
            for (WebElement removeLink : removeLinks) {
                removeLink.click();
                waitFor(ExpectedConditions.elementToBeClickable(By.id("btn-remove-template")));
                if (this.driver instanceof FirefoxDriver) {
                    // Firefox hack for modal screens.
                    findById("btn-remove-template").click();
                }
                findById("btn-remove-template").click();
                waitForHide("modal-template-remove");
            }
            waitForAjaxToComplete();
        });

        When("The user saves the template", () -> findById("btn-save-template").click());
        Then("A template with the name \"(.*)\" should be present", (String templateName) -> waitFor(ExpectedConditions.presenceOfNestedElementLocatedBy(By.id("list-template-links"), By.xpath("./li/a[text()='" + templateName + "']"))));

        And("The user applies the template \"(.*)\"", (String templateName) -> findById("list-template-links").findElement(By.xpath("./li/a[text()='" + templateName + "']")).click());
        Then("^The query field should contain the value \"(.*)\"$", (String expectedValue) -> assertEquals(expectedValue, findById("query-string").getAttribute("value")));


        final String eventId = UUID.randomUUID().toString();
        Given("The system contains events that form chain", () -> addTransactionsToEtm(eventId));
        When("The user searches for the first event in the chain", () -> setTextToElement(findById("query-string"), "_id: " + eventId));
        And("The event is found", () -> waitFor(c -> {
            findById("btn-search").click();
            waitForAjaxToComplete();
            return findById(eventId) != null;
        }));
        And("The user selects the event", () -> findById(eventId).click());
        Then("The \"(.*)\" tab should be selectable", (String tabName) -> {
            waitFor(ExpectedConditions.presenceOfElementLocated(By.xpath("//ul[@id='event-tabs']/li/a[text()='" + tabName + "']")));
            WebElement headerLink = findById("event-tabs").findElement(By.xpath("./li/a[text()='" + tabName + "']"));
            waitForShow(headerLink.getAttribute("id"));
        });
        When("The user selects the \"(.*)\" tab", (String tabName) -> findById("event-tabs").findElement(By.xpath("./li/a[text()='" + tabName + "']")).click());
        Then("The endpoint overview should be visible", () -> waitForShow("endpoint-overview"));
        And("The endpoint overview should contain (\\d+) canvas items", (Integer canvasCount) ->
                assertEquals(canvasCount.intValue(), findById("endpoint-overview").findElements(By.tagName("canvas")).size())
        );

        When("The users selects the application in the overview", () -> {
            List<WebElement> canvasElements = findById("endpoint-overview").findElements(By.tagName("canvas"));
            WebElement canvas = canvasElements.get(0);
            int clickPointXOffset = (canvas.getSize().getWidth() / 6) * 5;
            int clickPointYOffset = (canvas.getSize().getHeight() / 3) * 2;
            if (this.driver instanceof FirefoxDriver) {
                // Firefox offset positioning is done from the center of the canvas instead of the top left corner;
                clickPointXOffset = (canvas.getSize().getWidth() / 6) * 2;
                clickPointYOffset = 0;
            }
            new Actions(this.driver).moveToElement(canvas, clickPointXOffset, clickPointYOffset).click().perform();
        });

        Then("The transaction details should be visible", () -> {
            waitForShow("transaction-detail-table");
            // Make sure 6 events are in the transaction (and 1 for the table header).
            waitFor(c -> 8 == findById("transaction-detail-table").findElements(By.tagName("tr")).size());
        });
        Then("The event chain should be visible", () -> waitForShow("event-chain", false));
        And("The event chain should contain (\\d+) canvas items", (Integer canvasCount) ->
                waitFor(c -> findById("event-chain").findElements(By.tagName("canvas")).size() == canvasCount));
    }

    private void addTransactionsToEtm(String eventId) throws IOException {
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
        ZonedDateTime timestamp = ZonedDateTime.now();
        guiEndpointHandler.setHandlingTime(timestamp);
        guiEndpointHandler.setSequenceNumber(-1);
        guiEndpointHandler
                .addMetadata("User-Agent", "Mozilla/5.0 (X11; Fedora; Linux x86_64; rv:46.0) Gecko/20100101 Firefox/46.0")
                .addMetadata("Pragma", "no-cache");
        assertTrue(sendEventToEtm("http", this.httpEventWriter.write(new HttpTelemetryEventBuilder()
                .setId(eventId)
                .setPayload("GET http://www.my-company.com/shopping-card.html")
                .setName("GetShoppingCard")
                .setPayloadFormat(PayloadFormat.TEXT)
                .setHttpEventType(HttpTelemetryEvent.HttpEventType.GET)
                .setExpiry(timestamp.plusSeconds(30))
                .addMetadata("BusinessProcess", "User views shopping card")
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("/shopping-card.html")
                        .addEndpointHandler(guiEndpointHandler.setType(EndpointHandler.EndpointHandlerType.READER))
                )
                .build())));

        // Add some logging generated by our gui app.
        guiEndpointHandler.setSequenceNumber(1);
        assertTrue(sendEventToEtm("log", this.logEventWriter.write(new LogTelemetryEventBuilder()
                .setId(UUID.randomUUID().toString())
                .setPayload("Found user")
                .setPayloadFormat(PayloadFormat.TEXT)
                .setLogLevel("DEBUG")
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("com.my-company.gui.Httphandler.handleRequest(Httphandler.java:60")
                        .addEndpointHandler(guiEndpointHandler.setType(EndpointHandler.EndpointHandlerType.WRITER))
                )
                .build())));

        guiEndpointHandler.setSequenceNumber(0);
        assertTrue(sendEventToEtm("log", this.logEventWriter.write(new LogTelemetryEventBuilder()
                .setId(UUID.randomUUID().toString())
                .setPayload("User is requesting his/her shopping card.")
                .setPayloadFormat(PayloadFormat.TEXT)
                .setLogLevel("DEBUG")
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("com.my-company.gui.Httphandler.handleRequest(Httphandler.java:59)")
                        .addEndpointHandler(guiEndpointHandler.setType(EndpointHandler.EndpointHandlerType.WRITER))
                )
                .build())));


        timestamp = timestamp.plus(8, ChronoUnit.MILLIS);
        guiEndpointHandler.setHandlingTime(timestamp);
        guiEndpointHandler.setSequenceNumber(3);
        assertTrue(sendEventToEtm("log", this.logEventWriter.write(new LogTelemetryEventBuilder()
                .setId(UUID.randomUUID().toString())
                .setPayload("Requesting shoppping card over MQ.")
                .setPayloadFormat(PayloadFormat.TEXT)
                .setLogLevel("DEBUG")
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("com.my-company.gui.MqRequestor.requestShoppingCar(MqRequestor.java:352)")
                        .addEndpointHandler(guiEndpointHandler.setType(EndpointHandler.EndpointHandlerType.WRITER))
                )
                .build())));

        // Now let the gui app send an MQ request to the backend app.
        timestamp = timestamp.plus(1, ChronoUnit.MILLIS);
        guiEndpointHandler.setHandlingTime(timestamp);
        guiEndpointHandler.setSequenceNumber(null);
        timestamp = timestamp.plus(3, ChronoUnit.MILLIS);
        backendEndpointHandler.setHandlingTime(timestamp);
        String mqMessageId = UUID.randomUUID().toString();
        assertTrue(sendEventToEtm("messaging", this.mqEventWriter.write(new MessagingTelemetryEventBuilder()
                .setId(mqMessageId)
                .setPayload("<shoppingcard_request><customer_id>543214</customer_id></shoppingcard_request>")
                .setPayloadFormat(PayloadFormat.XML)
                .setMessagingEventType(MessagingTelemetryEvent.MessagingEventType.REQUEST)
                .setName("ShoppingCardRequest")
                .setExpiry(timestamp.plusSeconds(30))
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("BACKEND.QUEUE.1")
                        .addEndpointHandler(guiEndpointHandler.setType(EndpointHandler.EndpointHandlerType.WRITER))
                        .addEndpointHandler(backendEndpointHandler.setType(EndpointHandler.EndpointHandlerType.READER))
                )
                .build())));

        // Add some backend logging
        timestamp = timestamp.plus(7, ChronoUnit.MILLIS);
        backendEndpointHandler.setHandlingTime(timestamp);
        assertTrue(sendEventToEtm("log", this.logEventWriter.write(new LogTelemetryEventBuilder()
                .setId(UUID.randomUUID().toString())
                .setPayload("Received shopping card request.")
                .setPayloadFormat(PayloadFormat.TEXT)
                .setLogLevel("DEBUG")
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("com.my-company.backend.MqHandler.handleRequest(MqHandler.java:103)")
                        .addEndpointHandler(backendEndpointHandler.setType(EndpointHandler.EndpointHandlerType.WRITER))
                )
                .build())));

        // Request the shopping card from the db.
        timestamp = timestamp.plus(10, ChronoUnit.MILLIS);
        backendEndpointHandler.setHandlingTime(timestamp);
        String sqlRequestId = UUID.randomUUID().toString();
        assertTrue(sendEventToEtm("sql", this.sqlEventWriter.write(new SqlTelemetryEventBuilder()
                .setPayload("select * from shoppingcard where customerId = ?")
                .setId(sqlRequestId)
                .setDbQueryEventType(SqlTelemetryEvent.SqlEventType.SELECT)
                .setPayloadFormat(PayloadFormat.SQL)
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("TAB_CUSTOMER")
                        .addEndpointHandler(backendEndpointHandler.setType(EndpointHandler.EndpointHandlerType.WRITER))
                )
                .build())));
        timestamp = timestamp.plus(275, ChronoUnit.MILLIS);
        backendEndpointHandler.setHandlingTime(timestamp);
        assertTrue(sendEventToEtm("sql", this.sqlEventWriter.write(new SqlTelemetryEventBuilder()
                .setId(UUID.randomUUID().toString())
                .setCorrelationId(sqlRequestId)
                .setPayload("found 10 results")
                .setDbQueryEventType(SqlTelemetryEvent.SqlEventType.RESULTSET)
                .setPayloadFormat(PayloadFormat.SQL)
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("TAB_CUSTOMER")
                        .addEndpointHandler(backendEndpointHandler.setType(EndpointHandler.EndpointHandlerType.READER))
                )
                .build())));

        // Send a MQ response back to the gui app.
        timestamp = timestamp.plus(30, ChronoUnit.MILLIS);
        backendEndpointHandler.setHandlingTime(timestamp);
        timestamp = timestamp.plus(4, ChronoUnit.MILLIS);
        guiEndpointHandler.setHandlingTime(timestamp);
        assertTrue(sendEventToEtm("messaging", this.mqEventWriter.write(new MessagingTelemetryEventBuilder()
                .setId(UUID.randomUUID().toString())
                .setCorrelationId(mqMessageId)
                .setPayload("<shoppingcard_response><customer_id>543214</customer_id></shoppingcard_response>")
                .setPayloadFormat(PayloadFormat.XML)
                .setName("ShoppingCardResponse")
                .setMessagingEventType(MessagingTelemetryEvent.MessagingEventType.RESPONSE)
                .setExpiry(timestamp.plusSeconds(30))
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("FRONTEND.QUEUE.1")
                        .addEndpointHandler(backendEndpointHandler.setType(EndpointHandler.EndpointHandlerType.WRITER))
                        .addEndpointHandler(guiEndpointHandler.setType(EndpointHandler.EndpointHandlerType.READER))
                )
                .build())));

        // And finally let the gui app return the html page.
        timestamp = timestamp.plus(32, ChronoUnit.MILLIS);
        guiEndpointHandler.setHandlingTime(timestamp);
        assertTrue(sendEventToEtm("http", this.httpEventWriter.write(new HttpTelemetryEventBuilder()
                .setId(UUID.randomUUID().toString())
                .setCorrelationId(eventId)
                .setPayload("<html><body><p>We found 2 items in your shopping card</p></body></html>")
                .setPayloadFormat(PayloadFormat.HTML)
                .setName("ReturnShoppingCard")
                .setHttpEventType(HttpTelemetryEvent.HttpEventType.RESPONSE)
                .setStatusCode(200)
                .addOrMergeEndpoint(new EndpointBuilder()
                        .addEndpointHandler(guiEndpointHandler.setType(EndpointHandler.EndpointHandlerType.WRITER))
                )
                .build())));
    }


    private void selectQueryTypes(String types) {
        // First deselect everything.
        find(By.xpath("//label[@for='check-type-business']")).click();
        find(By.xpath("//label[@for='check-type-http']")).click();
        find(By.xpath("//label[@for='check-type-log']")).click();
        find(By.xpath("//label[@for='check-type-messaging']")).click();
        find(By.xpath("//label[@for='check-type-sql']")).click();
        String[] splittedTypes = types.toLowerCase().split(",");
        for (String type : splittedTypes) {
            find(By.xpath("//label[@for='check-type-" + type + "']")).click();
        }
    }


}
