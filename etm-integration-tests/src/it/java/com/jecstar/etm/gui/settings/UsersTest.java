package com.jecstar.etm.gui.settings;

import com.jecstar.etm.gui.AbstractIntegrationTest;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.principal.SecurityRoles;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class UsersTest extends AbstractIntegrationTest {
	
	@Test
	public void testRemoveLatestAdminAccount() {
		getSecurePage(this.httpHost + "/gui/settings/users.html", c -> ExpectedConditions.visibilityOf(findById("input-user-id")), 1000);
		
		Select userSelect = new Select(findById("sel-user"));
		// Remove all users except the admin
		List<WebElement> options = userSelect.getOptions();
		for (WebElement user : options) {
			if (user.getAttribute("value") == null || "".equals(user.getAttribute("value").trim())) {
				// Skip the empty option
				continue;
			}
			if (!"admin".equals(user.getAttribute("value"))) {
				userSelect.selectByValue(user.getAttribute("value"));
				waitFor(ExpectedConditions.elementToBeClickable(By.id("btn-confirm-remove-user")));
				findById("btn-confirm-remove-user").click();
				waitForShow("modal-user-remove", false);
				findById("btn-remove-user").click();
				waitForHide("modal-user-remove");
			}
		}

        waitFor(ExpectedConditions.elementToBeClickable(findById("sel-user")));
		userSelect.selectByValue("admin");

		// Make sure the admin data is loaded.
		assertEquals("admin", findById("input-user-id").getAttribute("value"));
		// Make sure the user settings write role is loaded.
		Select userSettingRoleSelect = new Select(findById("sel-user-settings-acl"));
		assertEquals(SecurityRoles.USER_SETTINGS_READ_WRITE, userSettingRoleSelect.getFirstSelectedOption().getAttribute("value"));

		// Now remove the user settings write role.
		userSettingRoleSelect.selectByValue("none");

		// Try to save the user, this should not work because we lose all admins.
		findById("btn-confirm-save-user").click();
		// Wait for the confirmation button to show up
		waitForShow("modal-user-overwrite", false);
		findById("btn-save-user").click();
		waitForHide("modal-user-overwrite");
		
		// An error box should be shows.
		waitForShow("users_errorBox");
		// And make sure the error box contains the correct error.
		assertTrue(findById("users_errorBox").getText().contains("" + EtmException.NO_MORE_USER_ADMINS_LEFT));

		// Somehow the remove button isn't enabled at this point
		waitFor(ExpectedConditions.elementToBeClickable(By.id("btn-confirm-remove-user")));
		
		// Now try to remove the user
		findById("btn-confirm-remove-user").click();
		// Wait for the confirmation button to show up
		waitForShow("modal-user-remove", false);
		findById("btn-remove-user").click();
		waitForHide("modal-user-remove");
		
		// An error box should be shows.
		waitForShow("users_errorBox");
		// And make sure the error box contains the correct error.
		assertTrue(findById("users_errorBox").getText().contains("" + EtmException.NO_MORE_USER_ADMINS_LEFT));
	}

}
