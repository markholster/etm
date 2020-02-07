package com.jecstar.etm.gui.settings;

import com.consol.citrus.annotations.CitrusEndpoint;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.dsl.junit.jupiter.CitrusExtension;
import com.consol.citrus.dsl.runner.TestRunner;
import com.consol.citrus.selenium.endpoint.SeleniumBrowser;
import com.jecstar.etm.integration.test.core.AbstractCitrusSeleniumTest;
import com.jecstar.etm.integration.test.core.EmbeddableLdapServer;
import com.jecstar.etm.server.core.domain.principal.SecurityRoles;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Month;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CitrusExtension.class)
public class ClusterSettingsIntegrationTest extends AbstractCitrusSeleniumTest {

    private final String clusterPath = "/gui/settings/cluster.html";

    @CitrusEndpoint(name = "firefox")
    private SeleniumBrowser firefox;

    @CitrusEndpoint(name = "chrome")
    private SeleniumBrowser chrome;

    private EmbeddableLdapServer server;

    @BeforeEach
    private void setup() {
        this.server = new EmbeddableLdapServer();
        this.server.startServer();
    }

    @AfterEach
    private void tearDown() {
        if (this.server != null) {
            this.server.stopServer();
            this.server = null;
        }
    }

    @AfterAll
    private void afterAll() {
        this.firefox.stop();
        this.chrome.stop();
    }

    @Test
    @CitrusTest
    public void testLdapSettingsInFirefox(@CitrusResource TestRunner runner) {
        setTestMetadata(runner, "Mark Holster", "Test the ldap settings functionality in Firefox", 2018, Month.OCTOBER, 20);
        testLdapSettings(runner, this.firefox);
    }

    @Test
    @CitrusTest
    public void testLdapSettingsInChrome(@CitrusResource TestRunner runner) {
        setTestMetadata(runner, "Mark Holster", "Test the ldap settings functionality in Chrome", 2018, Month.OCTOBER, 20);
        testLdapSettings(runner, this.chrome);
    }

    private void testLdapSettings(TestRunner runner, SeleniumBrowser browser) {
        final String adminId = EmbeddableLdapServer.ADMIN_USER_ID;
        final String adminGroup = EmbeddableLdapServer.ADMIN_GROUP_DN;

        login(runner, browser);
        runner.selenium(action -> action.navigate(getEtmUrl() + clusterPath));
        runner.selenium(action -> action.waitUntil().visible().element(By.id("cluster_box")));
        // Select the LDAP tab
        runner.selenium(action -> action.click().element(By.xpath("//a[@class='nav-link' and text()='Ldap']")));
        // Remove the current LDAP configuration when present
        WebElement removeLdapButton = browser.getWebDriver().findElement(By.id("btn-confirm-remove-ldap"));
        if (removeLdapButton.isEnabled()) {
            runner.selenium(action -> action.waitUntil().visible().element(By.id("btn-confirm-remove-ldap")));
            runner.selenium(action -> action.click().element(By.id("btn-confirm-remove-ldap")));
            // Wait and configurm the confirm removal popup.
            waitForModalToShow(runner, "Confirm removal");
            confirmModalWith(runner, "Confirm removal", "Yes");
            waitForModalToHide(runner, "Confirm removal");
            waitForAjaxToComplete(runner);
        }
        runner.selenium(action -> action.waitUntil().visible().element(By.id("input-ldap-host")));
        // Set all values from the EmbeddableLdapServer
        runner.selenium(action -> action.setInput(EmbeddableLdapServer.HOST).element(By.id("input-ldap-host")));
        runner.selenium(action -> action.setInput("" + EmbeddableLdapServer.PORT).element(By.id("input-ldap-port")));
        runner.selenium(action -> action.setInput(EmbeddableLdapServer.BIND_DN).element(By.id("input-ldap-bind-dn")));
        runner.selenium(action -> action.setInput(EmbeddableLdapServer.BIND_PASSWORD).element(By.id("input-ldap-bind-password")));
        runner.selenium(action -> action.setInput(EmbeddableLdapServer.BIND_DN).element(By.id("input-ldap-connection-test-base-dn")));
        runner.selenium(action -> action.setInput("(objectClass=*)").element(By.id("input-ldap-connection-test-search-filter")));
        runner.selenium(action -> action.setInput(EmbeddableLdapServer.GROUP_BASE_DN).element(By.id("input-ldap-group-base-dn")));
        runner.selenium(action -> action.setInput("(cn={group})").element(By.id("input-ldap-group-search-filter")));
        runner.selenium(action -> action.setInput(EmbeddableLdapServer.USER_BASE_DN).element(By.id("input-ldap-user-base-dn")));
        runner.selenium(action -> action.setInput("(uid={user})").element(By.id("input-ldap-user-search-filter")));
        runner.selenium(action -> action.setInput(EmbeddableLdapServer.USER_ID_ATTRIBUTE).element(By.id("input-ldap-user-id-attribute")));
        runner.selenium(action -> action.setInput(EmbeddableLdapServer.USER_NAME_ATTRIBUTE).element(By.id("input-ldap-user-fullname-attribute")));
        runner.selenium(action -> action.setInput(EmbeddableLdapServer.USER_EMAIL_ATTRIBUTE).element(By.id("input-ldap-user-email-attribute")));
        runner.selenium(action -> action.setInput(EmbeddableLdapServer.GROUP_BASE_DN).element(By.id("input-ldap-user-groups-query-base-dn")));
        runner.selenium(action -> action.setInput("(|(member={dn})(uniqueMember={dn})(memberUid={uid}))").element(By.id("input-ldap-user-groups-query-filter")));
        // And save the configuration
        runner.selenium(action -> action.click().element(By.id("btn-save-ldap")));
        // Now browse to the users page
        runner.selenium(action -> action.navigate(getEtmUrl() + "/gui/settings/users.html"));
        waitForAjaxToComplete(runner);
        // And check if the import button is enabled.
        runner.selenium(action -> action.find().element(By.id("btn-confirm-import-user")).enabled(true));
        // Remove the etm-admin user if it is present.
        selectAndRemoveItem(runner, browser, "user", adminId);
        waitForAjaxToComplete(runner);
        // Now start the import of the user.
        waitForClickable(browser, By.id("btn-confirm-import-user"));
        runner.selenium(action -> action.click().element(By.id("btn-confirm-import-user")));
        waitForModalToShow(runner, "Import LDAP user");
        runner.selenium(action -> action.setInput(adminId).element(By.id("input-import-user-id")));
        confirmModalWith(runner, "Import LDAP user", "Import");
        waitForModalToHide(runner, "Import LDAP user");
        waitForAjaxToComplete(runner);
        // The user should be available.
        runner.selenium(action -> action.select(adminId).element(By.id("sel-user")));
        runner.selenium(action -> action.find().element(By.id("input-user-id")).enabled(false).element("value", adminId));

        // Now test the import of groups
        runner.selenium(action -> action.navigate(getEtmUrl() + "/gui/settings/groups.html"));
        waitForAjaxToComplete(runner);
        // And check if the import button is enabled.
        runner.selenium(action -> action.find().element(By.id("btn-confirm-import-group")).enabled(true));
        // Remove the admin group if it is present.
        selectAndRemoveItem(runner, browser, "group", adminGroup);
        // Now start the import of the group.
        waitForClickable(browser, By.id("btn-confirm-import-group"));
        runner.selenium(action -> action.click().element(By.id("btn-confirm-import-group")));
        waitForModalToShow(runner, "Import LDAP group");
        runner.selenium(action -> action.select(adminGroup).element(By.id("sel-import-group")));
        confirmModalWith(runner, "Import LDAP group", "Import");
        waitForModalToHide(runner, "Import LDAP group");
        waitForAjaxToComplete(runner);
        // Select the imported group and enable the event read role to it.
        runner.selenium(action -> action.select(adminGroup).element(By.id("sel-group")));
        runner.selenium(action -> action.select(SecurityRoles.ETM_EVENT_READ).element(By.id("sel-event-acl")));
        runner.selenium(action -> action.click().element(By.id("btn-confirm-save-group")));
        waitForModalToShow(runner, "Group already exists");
        confirmModalWith(runner, "Group already exists", "Yes");
        waitForModalToHide(runner, "Group already exists");
        waitForAjaxToComplete(runner);
        // Now logouot and login with the LDAP user.
        runner.selenium(action -> action.navigate(getEtmUrl() + "/gui/logout?source=./"));
        login(runner, browser, EmbeddableLdapServer.ADMIN_USER_ID, "password");
        runner.selenium(action -> action.navigate(getEtmUrl() + "/gui/search/index.html"));
        // Check if the search container is visible.
        runner.selenium(action -> action.waitUntil().visible().element(By.id("search-container")));
    }
}
