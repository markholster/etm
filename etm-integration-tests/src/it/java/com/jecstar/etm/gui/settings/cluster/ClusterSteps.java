package com.jecstar.etm.gui.settings.cluster;

import com.jecstar.etm.gui.AbstractSteps;
import com.jecstar.etm.server.core.domain.principal.SecurityRoles;
import com.jecstar.etm.server.core.ldap.EmbeddableLdapServer;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unused")
public class ClusterSteps extends AbstractSteps {

    private EmbeddableLdapServer server;

    public ClusterSteps() {


        Before(new String[]{"@LdapServer"}, c -> {
            this.server = new EmbeddableLdapServer();
            this.server.startServer();
        });

        After(new String[]{"@LdapServer"}, c -> {
            if (this.server != null) {
                this.server.stopServer();
                this.server = null;
            }
        });

        Then("The cluster settings page should be visible", () -> {
            waitForAjaxToComplete();
            assertNotNull(findById("cluster_box"));
        });

        When("The user selects the (.*) tab", (String tabName) -> {
            WebElement tabLink = find(By.xpath("//a[@class='nav-link' and text()='" + tabName + "']"));
            clickOnElement(tabLink);
            String areaId = tabLink.getAttribute("aria-controls");
            waitForShow(areaId, false);
        });

        And("The current Ldap configuration is empty", () -> {
            WebElement removeLdapButton = findById("btn-confirm-remove-ldap");
            if (removeLdapButton.isEnabled()) {
                removeLdapButton.click();
                // Wait for the confirm removal popup to show.
                waitForModalToShow("Confirm removal");
                confirmModalWith("Yes");
                // wait until the modal is hidden
                waitForModalToHide("Confirm removal");
                waitForAjaxToComplete();
            }
        });

        And("The embedded Ldap server is entered", () -> {
            setTextToElement(findById("input-ldap-host"), EmbeddableLdapServer.HOST);
            setTextToElement(findById("input-ldap-port"), "" + EmbeddableLdapServer.PORT);
            setTextToElement(findById("input-ldap-bind-dn"), EmbeddableLdapServer.BIND_DN);
            setTextToElement(findById("input-ldap-bind-password"), EmbeddableLdapServer.BIND_PASSWORD);

            setTextToElement(findById("input-ldap-connection-test-base-dn"), EmbeddableLdapServer.BIND_DN);
            setTextToElement(findById("input-ldap-connection-test-search-filter"), "(objectClass=*)");

            setTextToElement(findById("input-ldap-group-base-dn"), EmbeddableLdapServer.GROUP_BASE_DN);
            setTextToElement(findById("input-ldap-group-search-filter"), "(cn={group})");

            setTextToElement(findById("input-ldap-user-base-dn"), EmbeddableLdapServer.USER_BASE_DN);
            setTextToElement(findById("input-ldap-user-search-filter"), "(uid={user})");
            setTextToElement(findById("input-ldap-user-id-attribute"), EmbeddableLdapServer.USER_ID_ATTRIBUTE);
            setTextToElement(findById("input-ldap-user-fullname-attribute"), EmbeddableLdapServer.USER_NAME_ATTRIBUTE);
            setTextToElement(findById("input-ldap-user-email-attribute"), EmbeddableLdapServer.USER_EMAIL_ATTRIBUTE);

            setTextToElement(findById("input-ldap-user-groups-query-base-dn"), EmbeddableLdapServer.GROUP_BASE_DN);
            setTextToElement(findById("input-ldap-user-groups-query-filter"), "(| (member={dn}) (uniqueMember={dn}) (memberUid={uid}))");

            clickOnElement("btn-save-ldap");
            waitForAjaxToComplete();
        });

        Then("The import buttons should be enabled", () -> waitFor(ExpectedConditions.elementToBeClickable(By.id("btn-confirm-import-user"))));

        When("The (.*) with id \"(.*)\" is removed", (String itemType, String itemId) -> {
            Select itemSelect = new Select(findById("sel-" + itemType));
            if (itemSelect.getOptions().stream().anyMatch(p -> itemId.equals(p.getAttribute("value")))) {
                // The user/group is imported... remove it otherwise we cannot test the import.
                itemSelect.selectByValue(itemId);
                sleepWhenChrome(500);
                clickOnElement("btn-confirm-remove-" + itemType);
                waitForModalToShow("Confirm removal");
                confirmModalWith("Yes");
                waitForModalToHide("Confirm removal");
                waitForAjaxToComplete();
            }
        });

        And("The (.*) with id \"(.*)\" is imported from Ldap", (String itemType, String itemId) -> {
            sleepWhenChrome(500);
            clickOnElement("btn-confirm-import-" + itemType);
            waitForModalToShow("Import LDAP " + itemType);
            if ("user".equals(itemType)) {
                setTextToElement(findById("input-import-user-id"), itemId);
            } else if ("group".equals(itemType)) {
                findById("sel-import-group").sendKeys(itemId);
            }
            confirmModalWith("Import");
            waitForModalToHide("Import LDAP " + itemType);
            waitForAjaxToComplete();
        });

        Then("The (.*) with id \"(.*)\" should be available", (String itemType, String itemId) -> {
            Select userSelect = new Select(findById("sel-" + itemType));
            assertTrue(userSelect.getOptions().stream().anyMatch(p -> itemId.equals(p.getAttribute("value"))));
        });

        When("The group with id \"(.*)\" is assigned the (.*) role", (String groupId, String role) -> {
            Select userSelect = new Select(findById("sel-group"));
            userSelect.selectByValue(groupId);

            Select eventRoleSelect = new Select(findById("sel-event-acl"));
            eventRoleSelect.selectByValue(SecurityRoles.ETM_EVENT_READ);

            // Now save the group
            clickOnElement("btn-confirm-save-group");
            waitForModalToShow("Group already exists");
            confirmModalWith("Yes");
            waitForModalToHide("Group already exists");
            waitForAjaxToComplete();
        });

        Then("The search page should be visible", () ->
                assertNotNull(findById("search-container"))
        );

    }
}
