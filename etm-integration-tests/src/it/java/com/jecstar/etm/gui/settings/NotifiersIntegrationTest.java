package com.jecstar.etm.gui.settings;

import com.consol.citrus.annotations.CitrusEndpoint;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.dsl.junit.jupiter.CitrusExtension;
import com.consol.citrus.dsl.runner.TestRunner;
import com.consol.citrus.selenium.endpoint.SeleniumBrowser;
import com.jecstar.etm.gui.AbstractCitrusSeleniumTest;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmSnmpConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.snmp4j.PDU;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(CitrusExtension.class)
public class NotifiersIntegrationTest extends AbstractCitrusSeleniumTest {

    private final String notifiersPath = "/gui/settings/notifiers.html";

    @CitrusEndpoint(name = "firefox")
    private SeleniumBrowser firefox;

    @CitrusEndpoint(name = "chrome")
    private SeleniumBrowser chrome;

    private EmbeddableSnmpReceiver snmpReceiver;

    @AfterEach
    private void tearDown() {
        if (this.snmpReceiver != null) {
            this.snmpReceiver.stopServer();
            this.snmpReceiver = null;
        }
    }

    @Test
    @CitrusTest
    public void testSnmpNotifierInFirefox(@CitrusResource TestRunner runner) {
        setTestMetadata(runner, "Mark Holster", "Test the snmp notifier settings in Firefox", 2018, Month.OCTOBER, 21);
        testSnmpNotifier(runner, this.firefox);
    }

    @Test
    @CitrusTest
    public void testSnmpNotifierInChrome(@CitrusResource TestRunner runner) {
        setTestMetadata(runner, "Mark Holster", "Test the snmp notifier settings in Chrome", 2018, Month.OCTOBER, 21);
        testSnmpNotifier(runner, this.chrome);
    }

    private void testSnmpNotifier(TestRunner runner, SeleniumBrowser browser) {
        final String notifierName = "Integration Test SNMP Notifier";
        final String signalName = "Snmp Integration Test Signal";

        login(runner, browser);
        runner.selenium(action -> action.navigate(getEtmUrl() + notifiersPath));
        runner.selenium(action -> action.waitUntil().visible().element(By.id("notifiers_box")));
        waitForAjaxToComplete(runner);

        // Remove the notifier if present
        selectAndRemoveItem(runner, browser, "notifier", notifierName);
        waitForAjaxToComplete(runner);

        // Create the notifier
        runner.selenium(action -> action.setInput(notifierName).element(By.id("input-notifier-name")));
        runner.selenium(action -> action.select("SNMP").element(By.id("sel-notifier-type")));
        waitForClickable(browser, By.id("input-snmp-host"));
        runner.selenium(action -> action.setInput(EmbeddableSnmpReceiver.HOST).element(By.id("input-snmp-host")));
        runner.selenium(action -> action.setInput("" + EmbeddableSnmpReceiver.PORT).element(By.id("input-snmp-port")));
        runner.selenium(action -> action.select("V2C").element(By.id("sel-snmp-version")));
        runner.selenium(action -> action.setInput(EmbeddableSnmpReceiver.COMMUNITY).element(By.id("input-snmp-community")));
        waitForClickable(browser, By.id("btn-confirm-save-notifier"));
        runner.selenium(action -> action.click().element(By.id("btn-confirm-save-notifier")));
        waitForAjaxToComplete(runner);

        // Give elasticsearch some time to make the notifier loadable in the next page.
        sleep(3000);

        // Notifier created now give the user access to it
        runner.selenium(action -> action.navigate(getEtmUrl() + "/gui/settings/users.html"));
        waitForAjaxToComplete(runner);
        waitForClickable(browser, By.id("sel-user"));
        runner.selenium(action -> action.select(username).element(By.id("sel-user")));

        // Remove access to all notifiers
        sleepWhenChrome(browser, 1000);
        List<WebElement> elements = browser.getWebDriver().findElements(By.xpath("//*[@id='list-notifiers']/li[1]/div[1]/div/button"));
        while (elements.size() > 0) {
            elements.get(0).click();
            elements = browser.getWebDriver().findElements(By.xpath("//*[@id='list-notifiers']/li[1]/div[1]/div/button"));
        }
        sleepWhenChrome(browser, 1000);
        waitForClickable(browser, By.id("lnk-add-notifier"));
        runner.selenium(action -> action.click().element(By.id("lnk-add-notifier")));
        sleepWhenChrome(browser, 1000);
        waitForClickable(browser, By.xpath("//*[@id='list-notifiers']/li[1]/div[1]/select"));
        runner.selenium(action -> action.select(notifierName).element(By.xpath("//*[@id='list-notifiers']/li[1]/div[1]/select")));

        runner.selenium(action -> action.click().element(By.id("btn-confirm-save-user")));
        waitForModalToShow(runner, "User already exists");
        confirmModalWith(runner, "User already exists", "Yes");
        waitForModalToHide(runner, "User already exists");
        waitForAjaxToComplete(runner);

        // Give elasticsearch some time to make the notifier loadable in the next page.
        sleep(3000);

        // User has access now create a signal
        runner.selenium(action -> action.navigate(getEtmUrl() + "/gui/signal/signals.html"));
        waitForAjaxToComplete(runner);

        // Remove the signal if it is present.
        selectAndRemoveItem(runner, browser, "signal", signalName);
        waitForAjaxToComplete(runner);
        sleepWhenChrome(browser, 1000);

        // If no notifier currently added we add a new one.
        elements = browser.getWebDriver().findElements(By.xpath("//*[@id='list-notifiers']/li[1]/div[1]/select"));
        if (elements.isEmpty()) {
            waitForClickable(browser, By.id("lnk-add-notifier"));
            runner.selenium(action -> action.click().element(By.id("lnk-add-notifier")));
            sleepWhenChrome(browser, 1000);
        }

        // Create the signal
        runner.selenium(action -> action.setInput(signalName).element(By.id("input-signal-name")));
        runner.selenium(action -> action.select(ElasticsearchLayout.METRICS_INDEX_ALIAS_ALL).element(By.id("sel-data-source")));
        runner.selenium(action -> action.setInput("now-1h").element(By.id("input-signal-from")));
        runner.selenium(action -> action.setInput("now").element(By.id("input-signal-till")));
        runner.selenium(action -> action.setInput("timestamp").element(By.id("input-signal-time-filter-field")));
        runner.selenium(action -> action.setInput("*").element(By.id("input-signal-query")));

        runner.selenium(action -> action.click().element(By.id("btn-heading-threshold")));
        sleepWhenChrome(browser, 500);
        runner.selenium(action -> action.select("GTE").element(By.id("sel-signal-comparison")));
        runner.selenium(action -> action.setInput("1").element(By.id("input-signal-threshold")));
        runner.selenium(action -> action.setInput("5").element(By.id("input-signal-cardinality")));
        runner.selenium(action -> action.select("MINUTES").element(By.id("sel-signal-cardinality-timeunit")));
        // Remove all aggregators if present
        elements = browser.getWebDriver().findElements(By.xpath("//*[@data-element-type='remove-metrics-aggregator']"));
        while (elements.size() > 0) {
            elements.get(0).click();
            elements = browser.getWebDriver().findElements(By.xpath("//*[@data-element-type='remove-metrics-aggregator']"));
        }
        runner.selenium(action -> action.click().element(By.xpath("//*[@data-element-type='add-metrics-aggregator']")));
        runner.selenium(action -> action.setInput("Count").element(By.id("input-metrics-name-threshold-0-0")));
        runner.selenium(action -> action.select("count").element(By.id("sel-metrics-aggregator-threshold-0-0")));

        runner.selenium(action -> action.click().element(By.id("btn-heading-notifications")));
        sleepWhenChrome(browser, 500);
        runner.selenium(action -> action.setInput("1").element(By.id("input-signal-interval")));
        runner.selenium(action -> action.select("MINUTES").element(By.id("sel-signal-interval-timeunit")));
        runner.selenium(action -> action.setInput("1").element(By.id("input-signal-max-frequency-of-exceedance")));
        runner.selenium(action -> action.select(notifierName).element(By.className("etm-notifier")));

        // Visualize it
        sleepWhenChrome(browser, 2000);
        waitForClickable(browser, By.id("btn-visualize-signal"));
        runner.selenium(action -> action.click().element(By.id("btn-visualize-signal")));
        waitForAjaxToComplete(runner);
        sleepWhenChrome(browser, 2000);
        runner.selenium(action -> action.find().element(By.xpath("//*[@id='preview_box']/p[@class='text-danger']")).text("@contains('This would have triggered a notification.')@"));

        // Start the SNMP receiver and save the Signal.
        this.snmpReceiver = new EmbeddableSnmpReceiver();
        this.snmpReceiver.startServer();
        runner.selenium(action -> action.click().element(By.id("btn-confirm-save-signal")));
        // Now wait for the notification.
        PDU pdu = this.snmpReceiver.retrievePDU(75_000);
        assertEquals(PDU.NOTIFICATION, pdu.getType());
        OctetString variable = (OctetString) pdu.getVariable(new OID(EtmSnmpConstants.ETM_SIGNAL_NOTIFICATION_OID + EtmSnmpConstants.ETM_SIGNAL_NOTIFICATION_NAME_SUFFIX));
        assertEquals(signalName, new String(variable.getValue()));
    }
}
