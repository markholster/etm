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

package com.jecstar.etm.integration.test.core;

import com.consol.citrus.context.TestContext;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.selenium.actions.AbstractSeleniumAction;
import com.consol.citrus.selenium.endpoint.SeleniumBrowser;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriverException;

public class WaitForAjaxToCompleteAction extends AbstractSeleniumAction {

    private final long timeout = 10_000;

    public WaitForAjaxToCompleteAction() {
        super(("waitForAjaxToComplete"));
    }

    @Override
    protected void execute(SeleniumBrowser browser, TestContext context) {
        try {
            if (browser.getWebDriver() instanceof JavascriptExecutor) {
                JavascriptExecutor jsEngine = ((JavascriptExecutor) browser.getWebDriver());
                long start = System.currentTimeMillis();
                while (!(Boolean) jsEngine.executeScript("return jQuery.active == 0")) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    if (System.currentTimeMillis() - start > this.timeout) {
                        throw new CitrusRuntimeException("Timeout waiting for ajax to complete");
                    }
                }
            } else {
                log.warn("Skip waitForAjaxToComplete action because web driver is missing javascript features");
            }
        } catch (WebDriverException e) {
            throw new CitrusRuntimeException("Failed to execute JavaScript code", e);
        }
    }
}
