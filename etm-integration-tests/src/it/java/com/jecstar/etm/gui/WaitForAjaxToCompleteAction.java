package com.jecstar.etm.gui;

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
