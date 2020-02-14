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

package com.jecstar.etm.gui.settings;

import com.consol.citrus.annotations.CitrusEndpoint;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.dsl.junit.jupiter.CitrusExtension;
import com.consol.citrus.dsl.runner.TestRunner;
import com.consol.citrus.selenium.endpoint.SeleniumBrowser;
import com.jecstar.etm.integration.test.core.AbstractCitrusSeleniumTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CitrusExtension.class)
public class IndexStatisticsIntegrationTest extends AbstractCitrusSeleniumTest {

    private final String indexStatisticsPath = "/gui/settings/indexstats.html";

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
    public void testShowIndexStatisticsInFirefox(@CitrusResource TestRunner runner) {
        setTestMetadata(runner, "Mark Holster", "Test the display of the index statistics in Firefox", 2019, Month.FEBRUARY, 15);
        testShowIndexStatistics(runner, this.firefox);
    }

    @Test
    @CitrusTest
    public void testShowIndexStatisticsInChrome(@CitrusResource TestRunner runner) {
        setTestMetadata(runner, "Mark Holster", "Test the display of the index statistics in Chrome", 2019, Month.FEBRUARY, 15);
        testShowIndexStatistics(runner, this.chrome);
    }

    private void testShowIndexStatistics(TestRunner runner, SeleniumBrowser browser) {
        login(runner, browser);
        runner.selenium(action -> action.navigate(getEtmUrl() + indexStatisticsPath));
        waitForAjaxToComplete(runner);
        String value = browser.getWebDriver().findElement(By.id("text-total-events")).getText();
        assertTrue(value != null && value.length() > 0);
        value = value.replaceAll("[,.]", "");
        assertTrue(Long.valueOf(value) > 0);

        value = browser.getWebDriver().findElement(By.id("text-total-size")).getText();
        assertTrue(value != null && value.length() > 0);
        value = value.replaceAll("[,.]", "");
        assertTrue(Long.valueOf(value) > 0);

        List<WebElement> nodeElements = browser.getWebDriver().findElements(By.cssSelector("g.highcharts-series"));
        // Make sure we have 4 series. 1 for each bar graph, and 2 in the line graph.
        assertEquals(4, nodeElements.size());
    }
}