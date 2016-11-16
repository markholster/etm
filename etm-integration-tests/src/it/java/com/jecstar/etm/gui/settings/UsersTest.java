package com.jecstar.etm.gui.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openqa.selenium.support.ui.Select;

import com.jecstar.etm.gui.AbstractIntegrationTest;
import com.jecstar.etm.server.core.EtmException;

public class UsersTest extends AbstractIntegrationTest{
	
	@Test
	public void testRemoveLatestAdminAccount() {
		getSecurePage(this.httpHost + "/gui/settings/users.html", "input-user-id");
		
		Select userSelect = new Select(findById("sel-user"));
		// Make sure we have only a single user (and an empty option).
		assertSame(2, userSelect.getOptions().size());
		userSelect.selectByValue("admin");

		// Make sure the admin data is loaded.
		assertEquals("admin", findById("input-user-id").getAttribute("value"));
		// And make sure the admin role is selected.
		assertTrue(findById("check-role-admin").isSelected());
		
		// Now deselect the admin role checkbox to remove the admin role.
		findById("check-role-admin").click();
		assertFalse(findById("check-role-admin").isSelected());
		
		// Try to save the user, this should not work because we lose all admins.
		findById("btn-confirm-save-user").click();
		// Wait for the confirmation button to show up
		waitForShow("modal-user-overwrite");
		findById("btn-save-user").click();
		waitForHide("modal-user-overwrite");
		
		// An error box should be shows.
		waitForShow("users_errorBox");
		// And make sure the error box contains the correct error.
		findById("users_errorBox").getText().contains("" + EtmException.NO_MORE_ADMINS_LEFT);
		
		// Now try to remove the user
		findById("btn-confirm-remove-user").click();
		// Wait for the confirmation button to show up
		waitForShow("modal-user-remove");
		findById("btn-remove-user").click();
		waitForHide("modal-user-remove");
		
		// An error box should be shows.
		waitForShow("users_errorBox");
		// And make sure the error box contains the correct error.
		findById("users_errorBox").getText().contains("" + EtmException.NO_MORE_ADMINS_LEFT);
	}

}
