package com.jecstar.etm.gui.settings;

import com.jecstar.etm.gui.AbstractIntegrationTest;
import com.jecstar.etm.server.core.domain.principal.SecurityRoles;
import com.jecstar.etm.server.core.ldap.EmbeddableLdapServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class LdapTest extends AbstractIntegrationTest {

	private static EmbeddableLdapServer server;
	
	@BeforeAll
	public static void setupClass() {
		server = new EmbeddableLdapServer();
		server.startServer();
	}
	
	@AfterAll
	public static void tearDownClass() {
		if (server != null) {
			server.stopServer();
		}
	}
	
	@Test
	public void testSetupAndUseLdap() {
	    // remove user and group if already present.
		getSecurePage(this.httpHost + "/gui/settings/cluster.html", c -> ExpectedConditions.visibilityOf(findById("ldap-tab-header")), 1000);
		
		// Select the Ldap tab.
		findById("ldap-tab-header").click();
		waitForShow("input-ldap-host");
		
		// remove the current ldap configuration.
		removeLdapConfiguration();
		
		// Fill in the server configuration
		findById("input-ldap-host").sendKeys(EmbeddableLdapServer.HOST);
		findById("input-ldap-port").sendKeys("" + EmbeddableLdapServer.PORT);
		findById("input-ldap-bind-dn").sendKeys(EmbeddableLdapServer.BIND_DN);
		findById("input-ldap-bind-password").sendKeys(EmbeddableLdapServer.BIND_PASSWORD);
		
		findById("input-ldap-connection-test-base-dn").sendKeys(EmbeddableLdapServer.BIND_DN);
		findById("input-ldap-connection-test-search-filter").sendKeys("(objectClass=*)");
	
		findById("input-ldap-group-base-dn").sendKeys(EmbeddableLdapServer.GROUP_BASE_DN);
		findById("input-ldap-group-search-filter").sendKeys("(cn={group})");

		findById("input-ldap-user-base-dn").sendKeys(EmbeddableLdapServer.USER_BASE_DN);
		findById("input-ldap-user-search-filter").sendKeys("(uid={user})");
		findById("input-ldap-user-id-attribute").sendKeys(EmbeddableLdapServer.USER_ID_ATTRIBUTE);
		findById("input-ldap-user-fullname-attribute").sendKeys(EmbeddableLdapServer.USER_NAME_ATTRIBUTE);
		findById("input-ldap-user-email-attribute").sendKeys(EmbeddableLdapServer.USER_EMAIL_ATTRIBUTE);
		
		findById("input-ldap-user-groups-query-base-dn").sendKeys(EmbeddableLdapServer.GROUP_BASE_DN);
		findById("input-ldap-user-groups-query-filter").sendKeys("(| (member={dn}) (uniqueMember={dn}) (memberUid={uid}))");
		
		findById("btn-save-ldap").click();
		
		// Ldap configuration saved. Now import some users and groups.
        getSecurePage(this.httpHost + "/gui/settings/users.html", null, 1000);
		
		Select userSelect = new Select(findById("sel-user"));
		if (userSelect.getOptions().stream().anyMatch(p -> EmbeddableLdapServer.ADMIN_USER_ID.equals(p.getAttribute("value")))) {
			// The user is imported... remove it otherwise we cannot test the import.
			userSelect.selectByValue(EmbeddableLdapServer.ADMIN_USER_ID);
			findById("btn-confirm-remove-user").click();
			waitForShow("modal-user-remove", false);
			findById("btn-remove-user").click();
			waitForHide("modal-user-remove");
		}

		// Now we are going to import the user from ldap.
        waitFor(ExpectedConditions.elementToBeClickable(By.id("btn-confirm-import-user")));
        findById("btn-confirm-import-user").click();
		waitForShow("modal-user-import", false);
		// Set the user name
		findById("input-import-user-id").sendKeys(EmbeddableLdapServer.ADMIN_USER_ID);
		// And import the user
		findById("btn-import-user").click();
		waitForHide("modal-user-import");
		// Make sure the user is imported
		assertEquals(EmbeddableLdapServer.ADMIN_USER_ID,  findById("input-user-id").getAttribute("value"));
		
		// The user is imported, now import the group
        getSecurePage(this.httpHost + "/gui/settings/groups.html", null, 1000);
		Select groupSelect = new Select(findById("sel-group"));
		if (groupSelect.getOptions().stream().anyMatch(p -> EmbeddableLdapServer.ADMIN_GROUP_DN.equals(p.getAttribute("value")))) {
			// The group is imported... remove it otherwise we cannot test the import.
			groupSelect.selectByValue(EmbeddableLdapServer.ADMIN_GROUP_DN);
			findById("btn-confirm-remove-group").click();
			waitForShow("modal-group-remove", false);
			findById("btn-remove-group").click();
			waitForHide("modal-group-remove");
		}
		
		// Now we are going to import the group from ldap.
        waitFor(ExpectedConditions.elementToBeClickable(By.id("btn-confirm-import-group")));
        findById("btn-confirm-import-group").click();
		waitForShow("modal-group-import", false);
		findById("sel-import-group").sendKeys(EmbeddableLdapServer.ADMIN_GROUP_DN);
		findById("btn-import-group").click();
		waitForHide("modal-group-import");
		// Make sure the group is imported
		assertEquals(EmbeddableLdapServer.ADMIN_GROUP_DN,  findById("input-group-name").getAttribute("value"));

        // Assign event read role to the admin group.
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
		Select eventRoleSelect = new Select(findById("sel-event-acl"));
		eventRoleSelect.selectByValue(SecurityRoles.ETM_EVENT_READ);

        // Now save the group
		findById("btn-confirm-save-group").click();
		waitForShow("modal-group-overwrite", false);
		findById("btn-save-group").click();
		waitForHide("modal-group-overwrite");
		
		// Logout and login to check if it works.
		this.driver.navigate().to(this.httpHost + "/gui/logout?source=./");
	    this.driver.findElement(By.id("j_username")).sendKeys(EmbeddableLdapServer.ADMIN_USER_ID);     
	    this.driver.findElement(By.id("j_password")).sendKeys("password");     
	    this.driver.findElement(By.className("btn")).submit();
	    waitForShow("event-count");
	}
	
	private void removeLdapConfiguration() {
		WebElement removeLdapButton = findById("btn-confirm-remove-ldap");
		if (removeLdapButton.isEnabled()) {
			removeLdapButton.click();
			// Wait for the confirm removal popup to show.
			waitForShow("modal-ldap-remove", false);
			// Remove the ldap configuration.
			findById("btn-remove-ldap").click();
			// wait until the modal is hidden
			waitForHide("modal-ldap-remove");
		}
	}
}
