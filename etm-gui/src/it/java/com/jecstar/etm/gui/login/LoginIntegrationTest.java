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

package com.jecstar.etm.gui.login;

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

import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CitrusExtension.class)
public class LoginIntegrationTest extends AbstractCitrusSeleniumTest {


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
    public void testLoginWithFirefox(@CitrusResource TestRunner runner) {
        setTestMetadata(runner, "Mark Holster", "Test login functionality Firefox", 2018, Month.OCTOBER, 20);
        testLogin(runner, this.firefox);
    }

    @Test
    @CitrusTest
    public void testLoginWithChrome(@CitrusResource TestRunner runner) {
        setTestMetadata(runner, "Mark Holster", "Test login functionality Chrome", 2018, Month.OCTOBER, 20);
        testLogin(runner, this.chrome);
    }

    private void testLogin(TestRunner runner, SeleniumBrowser browser) {
        login(runner, browser);
        waitForAjaxToComplete(runner);
        String value = browser.getWebDriver().findElement(By.id("event-count")).getText();
        value = value.replaceAll("[,.]", "");
        assertTrue(Long.valueOf(value) > 0);
    }


}
