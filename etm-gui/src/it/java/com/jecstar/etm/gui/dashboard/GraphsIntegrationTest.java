package com.jecstar.etm.gui.dashboard;

import com.consol.citrus.annotations.CitrusEndpoint;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.dsl.junit.jupiter.CitrusExtension;
import com.consol.citrus.dsl.runner.TestRunner;
import com.consol.citrus.selenium.endpoint.SeleniumBrowser;
import com.jecstar.etm.integration.test.core.AbstractCitrusSeleniumTest;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CitrusExtension.class)
public class GraphsIntegrationTest extends AbstractCitrusSeleniumTest {

    private final String graphsPath = "/gui/dashboard/graphs.html";

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
    public void testAreaGraphInFirefox(@CitrusResource TestRunner runner) {
        setTestMetadata(runner, "Mark Holster", "Test the area graph in Firefox", 2019, Month.JANUARY, 5);
        testAreaGraph(runner, this.firefox);
    }

    @Test
    @CitrusTest
    public void testAreaGraphInChrome(@CitrusResource TestRunner runner) {
        setTestMetadata(runner, "Mark Holster", "Test the area graph in Chrome", 2019, Month.JANUARY, 5);
        testAreaGraph(runner, this.chrome);
    }

    private void testAreaGraph(TestRunner runner, SeleniumBrowser browser) {
        final String graphName = "Integration Test Memory Area Graph";
        loginAndRemoveGraph(runner, browser, graphName);

        // Fill the Data section
        fillDataSection(runner, ElasticsearchLayout.METRICS_INDEX_ALIAS_ALL, "", "now", "timestamp");

        // Fill the graph section
        runner.selenium(action -> action.click().element(By.id("btn-heading-graph")));
        sleepWhenChrome(browser, 500);
        runner.selenium(action -> action.select("area").element(By.id("sel-graph-type")));
        runner.selenium(action -> action.select("stacked").element(By.id("sel-graph-subtype")));
        runner.selenium(action -> action.select("VERTICAL").element(By.id("sel-graph-orientation")));
        runner.selenium(action -> action.select("false").element(By.id("sel-graph-show-markers")));
        runner.selenium(action -> action.select("false").element(By.id("sel-graph-show-data-labels")));
        runner.selenium(action -> action.select("true").element(By.id("sel-graph-show-legend")));

        // Fill the x-axis section
        runner.selenium(action -> action.click().element(By.id("btn-heading-x-axis")));
        sleepWhenChrome(browser, 500);
        runner.selenium(action -> action.select("date_histogram").element(By.id("sel-bucket-aggregator-x-axis-0-0")));
        runner.selenium(action -> action.setInput("timestamp").element(By.id("input-bucket-field-x-axis-0-0")));
        runner.selenium(action -> action.select("auto").element(By.id("sel-bucket-date-interval-x-axis-0-0")));
        runner.selenium(action -> action.setInput("1").element(By.id("input-bucket-min-doc-count-x-axis-0-0")));

        // Fill the y-axis section
        selectAndEmptyAggregatorSection(runner, browser, "y-axis");
        runner.selenium(action -> action.setInput("Memory usage").element(By.id("input-y-axis-title")));
        runner.selenium(action -> action.setInput(".2s").element(By.id("input-y-axis-format")));

        // Add the Eden metric
        runner.selenium(action -> action.click().element(By.xpath("//div[@id='acc-collapse-y-axis']//*[@data-element-type='add-metrics-aggregator']")));
        runner.selenium(action -> action.setInput("Eden").element(By.id("input-metrics-name-y-axis-0-0")));
        runner.selenium(action -> action.select("true").element(By.id("sel-metrics-show-on-graph-y-axis-0-0")));
        runner.selenium(action -> action.select("average").element(By.id("sel-metrics-aggregator-y-axis-0-0")));
        runner.selenium(action -> action.setInput("mem.pools.PS-Eden-Space.used").element(By.id("input-metrics-field-y-axis-0-0")));
        // Add the Survivor space metric
        runner.selenium(action -> action.click().element(By.xpath("//*[@data-element-type='add-metrics-aggregator']")));
        runner.selenium(action -> action.setInput("Survivor space").element(By.id("input-metrics-name-y-axis-0-1")));
        runner.selenium(action -> action.select("true").element(By.id("sel-metrics-show-on-graph-y-axis-0-1")));
        runner.selenium(action -> action.select("average").element(By.id("sel-metrics-aggregator-y-axis-0-1")));
        runner.selenium(action -> action.setInput("mem.pools.PS-Survivor-Space.used").element(By.id("input-metrics-field-y-axis-0-1")));
        // Add the Old gen space metric
        runner.selenium(action -> action.click().element(By.xpath("//*[@data-element-type='add-metrics-aggregator']")));
        runner.selenium(action -> action.setInput("Old gen").element(By.id("input-metrics-name-y-axis-0-2")));
        runner.selenium(action -> action.select("true").element(By.id("sel-metrics-show-on-graph-y-axis-0-2")));
        runner.selenium(action -> action.select("average").element(By.id("sel-metrics-aggregator-y-axis-0-2")));
        runner.selenium(action -> action.setInput("mem.pools.PS-Old-Gen.used").element(By.id("input-metrics-field-y-axis-0-2")));

        // Visualize it
        visualizeGraph(runner, browser);

        List<WebElement> elements = browser.getWebDriver().findElements(By.className("highcharts-area"));
        assertTrue(elements.size() > 0);
        elements = browser.getWebDriver().findElements(By.xpath("//*[contains(@class, 'highcharts-data-label')]"));
        assertSame(0, elements.size());
        // Save the graph
        saveGraph(runner, graphName);
    }

    @Test
    @CitrusTest
    public void testBarGraphInFirefox(@CitrusResource TestRunner runner) {
        setTestMetadata(runner, "Mark Holster", "Test the bar graph in Firefox", 2019, Month.JANUARY, 5);
        testBarGraph(runner, this.firefox);
    }

    @Test
    @CitrusTest
    public void testBarGraphInChrome(@CitrusResource TestRunner runner) {
        setTestMetadata(runner, "Mark Holster", "Test the bar graph in Chrome", 2019, Month.JANUARY, 5);
        testBarGraph(runner, this.chrome);
    }

    private void testBarGraph(TestRunner runner, SeleniumBrowser browser) {
        final String graphName = "Integration Test Audit Bar Graph";
        loginAndRemoveGraph(runner, browser, graphName);

        // Fill the Data section
        fillDataSection(runner, ElasticsearchLayout.AUDIT_LOG_INDEX_ALIAS_ALL, "", "", "");

        // Fill the graph section
        runner.selenium(action -> action.click().element(By.id("btn-heading-graph")));
        sleepWhenChrome(browser, 500);
        runner.selenium(action -> action.select("bar").element(By.id("sel-graph-type")));
        runner.selenium(action -> action.select("percentage").element(By.id("sel-graph-subtype")));
        runner.selenium(action -> action.select("HORIZONTAL").element(By.id("sel-graph-orientation")));
        runner.selenium(action -> action.select("true").element(By.id("sel-graph-show-legend")));

        // Fill the x-axis section
        runner.selenium(action -> action.click().element(By.id("btn-heading-x-axis")));
        sleepWhenChrome(browser, 500);
        runner.selenium(action -> action.select("term").element(By.id("sel-bucket-aggregator-x-axis-0-0")));
        runner.selenium(action -> action.setInput("object_type").element(By.id("input-bucket-field-x-axis-0-0")));
        runner.selenium(action -> action.setInput("5").element(By.id("input-bucket-term-top-x-axis-0-0")));
        runner.selenium(action -> action.select("_count").element(By.id("sel-bucket-term-order-by-x-axis-0-0")));
        runner.selenium(action -> action.select("desc").element(By.id("sel-bucket-term-order-x-axis-0-0")));
        runner.selenium(action -> action.setInput("1").element(By.id("input-bucket-min-doc-count-x-axis-0-0")));

        // Fill the y-axis section
        selectAndEmptyAggregatorSection(runner, browser, "y-axis");
        runner.selenium(action -> action.setInput("Percentage").element(By.id("input-y-axis-title")));
        runner.selenium(action -> action.setInput("d").element(By.id("input-y-axis-format")));

        // Add the user bucket
        runner.selenium(action -> action.click().element(By.xpath("//div[@id='acc-collapse-y-axis']//*[@data-element-type='add-bucket-aggregator']")));
        runner.selenium(action -> action.setInput("User").element(By.id("input-bucket-name-y-axis-0-0")));
        runner.selenium(action -> action.select("true").element(By.id("sel-bucket-show-on-graph-y-axis-0-0")));
        runner.selenium(action -> action.select("term").element(By.id("sel-bucket-aggregator-y-axis-0-0")));
        runner.selenium(action -> action.setInput("principal_id").element(By.id("input-bucket-field-y-axis-0-0")));
        runner.selenium(action -> action.setInput("5").element(By.id("input-bucket-term-top-y-axis-0-0")));
        runner.selenium(action -> action.select("_count").element(By.id("sel-bucket-term-order-by-y-axis-0-0")));
        runner.selenium(action -> action.select("desc").element(By.id("sel-bucket-term-order-y-axis-0-0")));
        runner.selenium(action -> action.setInput("1").element(By.id("input-bucket-min-doc-count-y-axis-0-0")));
        // Add the count sub aggregator
        runner.selenium(action -> action.click().element(By.xpath("//*[@data-aggregator-level='1']//*[@data-element-type='add-metrics-aggregator']")));
        runner.selenium(action -> action.setInput("Count").element(By.id("input-metrics-name-y-axis-1-0")));
        runner.selenium(action -> action.select("true").element(By.id("sel-metrics-show-on-graph-y-axis-1-0")));
        runner.selenium(action -> action.select("count").element(By.id("sel-metrics-aggregator-y-axis-1-0")));

        // Visualize it
        visualizeGraph(runner, browser);

        List<WebElement> elements = browser.getWebDriver().findElements(By.className("highcharts-series-group"));
        assertTrue(elements.size() > 0);

        // Save the graph
        saveGraph(runner, graphName);
    }

    @Test
    @CitrusTest
    public void testLineGraphInFirefox(@CitrusResource TestRunner runner) {
        setTestMetadata(runner, "Mark Holster", "Test the line graph in Firefox", 2019, Month.JANUARY, 5);
        testLineGraph(runner, this.firefox);
    }

    @Test
    @CitrusTest
    public void testLineGraphInChrome(@CitrusResource TestRunner runner) {
        setTestMetadata(runner, "Mark Holster", "Test the line graph in Chrome", 2019, Month.JANUARY, 5);
        testLineGraph(runner, this.chrome);
    }

    private void testLineGraph(TestRunner runner, SeleniumBrowser browser) {
        final String graphName = "Integration Test Heap Line Graph";
        loginAndRemoveGraph(runner, browser, graphName);

        // Fill the Data section
        fillDataSection(runner, ElasticsearchLayout.METRICS_INDEX_ALIAS_ALL, "now-1y", "now", "timestamp");

        // Fill the graph section
        runner.selenium(action -> action.click().element(By.id("btn-heading-graph")));
        sleepWhenChrome(browser, 500);
        runner.selenium(action -> action.select("line").element(By.id("sel-graph-type")));
        runner.selenium(action -> action.select("VERTICAL").element(By.id("sel-graph-orientation")));
        runner.selenium(action -> action.select("true").element(By.id("sel-graph-show-markers")));
        runner.selenium(action -> action.select("true").element(By.id("sel-graph-show-data-labels")));
        runner.selenium(action -> action.select("true").element(By.id("sel-graph-show-legend")));

        // Fill the x-axis section
        runner.selenium(action -> action.click().element(By.id("btn-heading-x-axis")));
        sleepWhenChrome(browser, 500);
        runner.selenium(action -> action.select("date_histogram").element(By.id("sel-bucket-aggregator-x-axis-0-0")));
        runner.selenium(action -> action.setInput("timestamp").element(By.id("input-bucket-field-x-axis-0-0")));
        runner.selenium(action -> action.select("auto").element(By.id("sel-bucket-date-interval-x-axis-0-0")));
        runner.selenium(action -> action.setInput("1").element(By.id("input-bucket-min-doc-count-x-axis-0-0")));

        // Fill the y-axis section
        selectAndEmptyAggregatorSection(runner, browser, "y-axis");
        runner.selenium(action -> action.setInput("Heap usage").element(By.id("input-y-axis-title")));
        runner.selenium(action -> action.setInput(".2s").element(By.id("input-y-axis-format")));

        // Add the node bucket
        runner.selenium(action -> action.click().element(By.xpath("//div[@id='acc-collapse-y-axis']//*[@data-element-type='add-bucket-aggregator']")));
        runner.selenium(action -> action.setInput("Node").element(By.id("input-bucket-name-y-axis-0-0")));
        runner.selenium(action -> action.select("true").element(By.id("sel-bucket-show-on-graph-y-axis-0-0")));
        runner.selenium(action -> action.select("term").element(By.id("sel-bucket-aggregator-y-axis-0-0")));
        runner.selenium(action -> action.setInput("node.name").element(By.id("input-bucket-field-y-axis-0-0")));
        runner.selenium(action -> action.setInput("4").element(By.id("input-bucket-term-top-y-axis-0-0")));
        runner.selenium(action -> action.select("_count").element(By.id("sel-bucket-term-order-by-y-axis-0-0")));
        runner.selenium(action -> action.select("desc").element(By.id("sel-bucket-term-order-y-axis-0-0")));
        // Add the heap sub aggregator
        runner.selenium(action -> action.click().element(By.xpath("//*[@data-aggregator-level='1']//*[@data-element-type='add-metrics-aggregator']")));
        runner.selenium(action -> action.setInput("Heap").element(By.id("input-metrics-name-y-axis-1-0")));
        runner.selenium(action -> action.select("true").element(By.id("sel-metrics-show-on-graph-y-axis-1-0")));
        runner.selenium(action -> action.select("average").element(By.id("sel-metrics-aggregator-y-axis-1-0")));
        runner.selenium(action -> action.setInput("mem.heap.used").element(By.id("input-metrics-field-y-axis-1-0")));
        // Add the non-heap sub aggregator
        runner.selenium(action -> action.click().element(By.xpath("//*[@data-aggregator-level='1']//*[@data-element-type='add-metrics-aggregator']")));
        runner.selenium(action -> action.setInput("Non-Heap").element(By.id("input-metrics-name-y-axis-1-1")));
        runner.selenium(action -> action.select("true").element(By.id("sel-metrics-show-on-graph-y-axis-1-1")));
        runner.selenium(action -> action.select("average").element(By.id("sel-metrics-aggregator-y-axis-1-1")));
        runner.selenium(action -> action.setInput("mem.non-heap.used").element(By.id("input-metrics-field-y-axis-1-1")));

        // Visualize it
        visualizeGraph(runner, browser);

        List<WebElement> elements = browser.getWebDriver().findElements(By.className("highcharts-series-group"));
        assertTrue(elements.size() > 0);
        elements = browser.getWebDriver().findElements(By.xpath("//*[contains(@class, 'highcharts-data-label')]"));
        assertTrue(elements.size() > 0);

        // Save the graph
        saveGraph(runner, graphName);
    }

    @Test
    @CitrusTest
    public void testNumberGraphInFirefox(@CitrusResource TestRunner runner) {
        setTestMetadata(runner, "Mark Holster", "Test the number graph in Firefox", 2019, Month.JANUARY, 5);
        testNumberGraph(runner, this.firefox);
    }

    @Test
    @CitrusTest
    public void testNumberGraphInChrome(@CitrusResource TestRunner runner) {
        setTestMetadata(runner, "Mark Holster", "Test the number graph in Chrome", 2019, Month.JANUARY, 5);
        testNumberGraph(runner, this.chrome);
    }

    private void testNumberGraph(TestRunner runner, SeleniumBrowser browser) {
        final String graphName = "Integration Test Event Count Graph";
        loginAndRemoveGraph(runner, browser, graphName);

        // Fill the Data section
        fillDataSection(runner, ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL, "", "now", "timestamp");

        // Fill the graph section
        runner.selenium(action -> action.click().element(By.id("btn-heading-graph")));
        sleepWhenChrome(browser, 500);
        runner.selenium(action -> action.select("number").element(By.id("sel-graph-type")));

        // Fill the parts section
        selectAndEmptyAggregatorSection(runner, browser, "parts");
        runner.selenium(action -> action.setInput("d").element(By.id("input-parts-format")));

        // Add the count bucket
        runner.selenium(action -> action.click().element(By.xpath("//div[@id='acc-collapse-parts']//*[@data-element-type='add-metrics-aggregator']")));
        runner.selenium(action -> action.setInput("Count").element(By.id("input-metrics-name-parts-0-0")));
        runner.selenium(action -> action.select("true").element(By.id("sel-metrics-show-on-graph-parts-0-0")));
        runner.selenium(action -> action.select("count").element(By.id("sel-metrics-aggregator-parts-0-0")));

        // Visualize it
        visualizeGraph(runner, browser);

        List<WebElement> elements = browser.getWebDriver().findElements(By.xpath("//*[name()='svg']/*[name()='text' and not(@class)]"));
        assertSame(1, elements.size());

        // Save the graph
        saveGraph(runner, graphName);
    }

    @Test
    @CitrusTest
    public void testPieGraphInFirefox(@CitrusResource TestRunner runner) {
        setTestMetadata(runner, "Mark Holster", "Test the pie graph in Firefox", 2019, Month.JANUARY, 5);
        testPieGraph(runner, this.firefox);
    }

    @Test
    @CitrusTest
    public void testPieGraphInChrome(@CitrusResource TestRunner runner) {
        setTestMetadata(runner, "Mark Holster", "Test the pie graph in Chrome", 2019, Month.JANUARY, 5);
        testPieGraph(runner, this.chrome);
    }

    private void testPieGraph(TestRunner runner, SeleniumBrowser browser) {
        final String graphName = "Integration Test Event Pie Graph";
        loginAndRemoveGraph(runner, browser, graphName);

        // Fill the Data section
        fillDataSection(runner, ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL, "", "", "");

        // Fill the graph section
        runner.selenium(action -> action.click().element(By.id("btn-heading-graph")));
        sleepWhenChrome(browser, 500);
        runner.selenium(action -> action.select("pie").element(By.id("sel-graph-type")));
        runner.selenium(action -> action.select("semi_circle").element(By.id("sel-graph-subtype")));
        runner.selenium(action -> action.select("false").element(By.id("sel-graph-show-data-labels")));
        runner.selenium(action -> action.select("true").element(By.id("sel-graph-show-legend")));

        // Fill the category section
        runner.selenium(action -> action.click().element(By.id("btn-heading-category")));
        sleepWhenChrome(browser, 500);
        runner.selenium(action -> action.select("term").element(By.id("sel-bucket-aggregator-category-0-0")));
        runner.selenium(action -> action.setInput("object_type").element(By.id("input-bucket-field-category-0-0")));
        runner.selenium(action -> action.setInput("10").element(By.id("input-bucket-term-top-category-0-0")));
        runner.selenium(action -> action.select("_key").element(By.id("sel-bucket-term-order-by-category-0-0")));
        runner.selenium(action -> action.select("asc").element(By.id("sel-bucket-term-order-category-0-0")));
        runner.selenium(action -> action.setInput("0").element(By.id("input-bucket-min-doc-count-category-0-0")));

        // Fill the parts section
        selectAndEmptyAggregatorSection(runner, browser, "parts");
        runner.selenium(action -> action.setInput("d").element(By.id("input-parts-format")));

        // Add the count bucket
        runner.selenium(action -> action.click().element(By.xpath("//div[@id='acc-collapse-parts']//*[@data-element-type='add-metrics-aggregator']")));
        runner.selenium(action -> action.setInput("Count").element(By.id("input-metrics-name-parts-0-0")));
        runner.selenium(action -> action.select("true").element(By.id("sel-metrics-show-on-graph-parts-0-0")));
        runner.selenium(action -> action.select("count").element(By.id("sel-metrics-aggregator-parts-0-0")));

        // Visualize it
        visualizeGraph(runner, browser);

        List<WebElement> elements = browser.getWebDriver().findElements(By.xpath("//*[contains(@class, 'highcharts-pie-series')]"));
        assertTrue(elements.size() > 0);

        // Save the graph
        saveGraph(runner, graphName);
    }

    @Test
    @CitrusTest
    public void testScatterGraphInFirefox(@CitrusResource TestRunner runner) {
        setTestMetadata(runner, "Mark Holster", "Test the scatter graph in Firefox", 2019, Month.JANUARY, 5);
        testScatterGraph(runner, this.firefox);
    }

    @Test
    @CitrusTest
    public void testScatterGraphInChrome(@CitrusResource TestRunner runner) {
        setTestMetadata(runner, "Mark Holster", "Test the scatter graph in Chrome", 2019, Month.JANUARY, 5);
        testScatterGraph(runner, this.chrome);
    }

    private void testScatterGraph(TestRunner runner, SeleniumBrowser browser) {
        final String graphName = "Integration Test Audit Scatter Graph";
        loginAndRemoveGraph(runner, browser, graphName);

        // Fill the Data section
        fillDataSection(runner, ElasticsearchLayout.AUDIT_LOG_INDEX_ALIAS_ALL, "", "", "");

        // Fill the graph section
        runner.selenium(action -> action.click().element(By.id("btn-heading-graph")));
        sleepWhenChrome(browser, 500);
        runner.selenium(action -> action.select("scatter").element(By.id("sel-graph-type")));
        runner.selenium(action -> action.select("VERTICAL").element(By.id("sel-graph-orientation")));
        runner.selenium(action -> action.select("false").element(By.id("sel-graph-show-data-labels")));
        runner.selenium(action -> action.select("true").element(By.id("sel-graph-show-legend")));

        // Fill the x-axis section
        runner.selenium(action -> action.click().element(By.id("btn-heading-x-axis")));
        sleepWhenChrome(browser, 500);
        runner.selenium(action -> action.select("term").element(By.id("sel-bucket-aggregator-x-axis-0-0")));
        runner.selenium(action -> action.setInput("object_type").element(By.id("input-bucket-field-x-axis-0-0")));
        runner.selenium(action -> action.setInput("10").element(By.id("input-bucket-term-top-x-axis-0-0")));
        runner.selenium(action -> action.select("_count").element(By.id("sel-bucket-term-order-by-x-axis-0-0")));
        runner.selenium(action -> action.select("desc").element(By.id("sel-bucket-term-order-x-axis-0-0")));
        runner.selenium(action -> action.setInput("1").element(By.id("input-bucket-min-doc-count-x-axis-0-0")));

        // Fill the y-axis section
        selectAndEmptyAggregatorSection(runner, browser, "y-axis");
        runner.selenium(action -> action.setInput("d").element(By.id("input-y-axis-format")));

        // Add the user bucket
        runner.selenium(action -> action.click().element(By.xpath("//div[@id='acc-collapse-y-axis']//*[@data-element-type='add-bucket-aggregator']")));
        runner.selenium(action -> action.setInput("User").element(By.id("input-bucket-name-y-axis-0-0")));
        runner.selenium(action -> action.select("true").element(By.id("sel-bucket-show-on-graph-y-axis-0-0")));
        runner.selenium(action -> action.select("term").element(By.id("sel-bucket-aggregator-y-axis-0-0")));
        runner.selenium(action -> action.setInput("principal_id").element(By.id("input-bucket-field-y-axis-0-0")));
        runner.selenium(action -> action.setInput("5").element(By.id("input-bucket-term-top-y-axis-0-0")));
        runner.selenium(action -> action.select("_count").element(By.id("sel-bucket-term-order-by-y-axis-0-0")));
        runner.selenium(action -> action.select("desc").element(By.id("sel-bucket-term-order-y-axis-0-0")));
        runner.selenium(action -> action.setInput("1").element(By.id("input-bucket-min-doc-count-y-axis-0-0")));
        // Add the count sub aggregator
        runner.selenium(action -> action.click().element(By.xpath("//*[@data-aggregator-level='1']//*[@data-element-type='add-metrics-aggregator']")));
        runner.selenium(action -> action.setInput("Count").element(By.id("input-metrics-name-y-axis-1-0")));
        runner.selenium(action -> action.select("true").element(By.id("sel-metrics-show-on-graph-y-axis-1-0")));
        runner.selenium(action -> action.select("count").element(By.id("sel-metrics-aggregator-y-axis-1-0")));

        // Visualize it
        visualizeGraph(runner, browser);

        List<WebElement> elements = browser.getWebDriver().findElements(By.xpath("//*[contains(@class, 'highcharts-scatter-series')]"));
        assertTrue(elements.size() > 0);

        // Save the graph
        saveGraph(runner, graphName);
    }

    private void saveGraph(TestRunner runner, String graphName) {
        runner.selenium(action -> action.setInput(graphName).element(By.id("input-graph-name")));
        runner.selenium(action -> action.click().element(By.id("btn-confirm-save-graph")));
        waitForAjaxToComplete(runner);
    }

    private void visualizeGraph(TestRunner runner, SeleniumBrowser browser) {
        sleepWhenChrome(browser, 2000);
        waitForClickable(browser, By.id("btn-preview-graph"));
        runner.selenium(action -> action.click().element(By.id("btn-preview-graph")));
        waitForAjaxToComplete(runner);
        sleepWhenChrome(browser, 2000);
    }

    private void loginAndRemoveGraph(TestRunner runner, SeleniumBrowser browser, String graphName) {
        login(runner, browser);

        // Browse to the graphs page.
        runner.selenium(action -> action.navigate(getEtmUrl() + this.graphsPath));
        waitForAjaxToComplete(runner);

        // Remove the graph if present
        selectAndRemoveItem(runner, browser, "graph", graphName);
        waitForAjaxToComplete(runner);
    }

    private void selectAndEmptyAggregatorSection(TestRunner runner, SeleniumBrowser browser, String sectionId) {
        runner.selenium(action -> action.click().element(By.id("btn-heading-" + sectionId)));
        sleepWhenChrome(browser, 500);
        // Remove all aggregators if present
        List<WebElement> elements = browser.getWebDriver().findElements(By.xpath("//*[@data-element-type='remove-metrics-aggregator']"));
        while (elements.size() > 0) {
            elements.get(0).click();
            elements = browser.getWebDriver().findElements(By.xpath("//*[@data-element-type='remove-metrics-aggregator']"));
        }
        elements = browser.getWebDriver().findElements(By.xpath("//*[@data-element-type='remove-bucket-aggregator']"));
        while (elements.size() > 0) {
            elements.get(0).click();
            elements = browser.getWebDriver().findElements(By.xpath("//*[@data-element-type='remove-bucket-aggregator']"));
        }
        elements = browser.getWebDriver().findElements(By.xpath("//*[@data-element-type='remove-pipeline-aggregator']"));
        while (elements.size() > 0) {
            elements.get(0).click();
            elements = browser.getWebDriver().findElements(By.xpath("//*[@data-element-type='remove-pipeline-aggregator']"));
        }
    }

    private void fillDataSection(TestRunner runner, String index, String from, String till, String filterField) {
        runner.selenium(action -> action.select(index).element(By.id("sel-data-source")));
        runner.selenium(action -> action.setInput(from).element(By.id("input-graph-from")));
        runner.selenium(action -> action.setInput(till).element(By.id("input-graph-till")));
        runner.selenium(action -> action.setInput(filterField).element(By.id("input-graph-time-filter-field")));
        runner.selenium(action -> action.setInput("*").element(By.id("input-graph-query")));
    }
}
