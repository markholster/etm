package com.jecstar.etm.gui.settings.users;

import com.jecstar.etm.gui.AbstractSteps;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.principal.SecurityRoles;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unused")
public class UsersSteps extends AbstractSteps {

    public UsersSteps() {
        Then("The users page should be visible", () -> {
            waitForAjaxToComplete();
            assertNotNull(findById("users_box"));
        });

        When("All users are given user admin read settings", () -> {
            Select userSelect = new Select(findById("sel-user"));
            // Remove all users except the admin
            List<WebElement> options = userSelect.getOptions();
            for (WebElement user : options) {
                if (user.getAttribute("value") == null || "".equals(user.getAttribute("value").trim())) {
                    // Skip the empty option
                    continue;
                }
                if (!username.equals(user.getAttribute("value"))) {
                    waitFor(ExpectedConditions.elementToBeClickable(By.id("sel-user")));
                    userSelect.selectByValue(user.getAttribute("value"));
                    sleepWhenChrome(500);

                    Select userSettingsAcl = new Select(findById("sel-user-settings-acl"));
                    if ("Read & write".equals(userSettingsAcl.getFirstSelectedOption().getText())) {
                        userSettingsAcl.selectByValue(SecurityRoles.USER_SETTINGS_READ);
                        clickOnElement("btn-confirm-save-user");
                        waitForModalToShow("User already exists");
                        confirmModalWith("Yes");
                        waitForModalToHide("User already exists");
                        waitForAjaxToComplete();
                    }
                }
            }
        });

        And("The user (.*) is selected", (String username) -> {
            Select userSelect = new Select(findById("sel-user"));
            userSelect.selectByValue(username);

        });

        And("The User Settings option is set to (.*)", (String value) -> {
            Select userSettingsAcl = new Select(findById("sel-user-settings-acl"));
            userSettingsAcl.selectByVisibleText(value);
        });

        Then("Saving the user should fail", () -> {
            clickOnElement("btn-confirm-save-user");
            waitForModalToShow("User already exists");
            confirmModalWith("Yes");
            waitForModalToHide("User already exists");
            waitForAjaxToComplete();
            waitForShow("users_errorBox");
            String errorText = find(By.id("users_errorBox")).getText();
            assertTrue(errorText.contains("" + EtmException.NO_MORE_USER_ADMINS_LEFT));
        });

        Then("Deleting the user should fail", () -> {
            clickOnElement("btn-confirm-remove-user");
            waitForModalToShow("Confirm removal");
            confirmModalWith("Yes");
            waitForModalToHide("Confirm removal");
            waitForAjaxToComplete();
            waitForShow("users_errorBox");
            String errorText = find(By.id("users_errorBox")).getText();
            assertTrue(errorText.contains("" + EtmException.NO_MORE_USER_ADMINS_LEFT));
        });

        And("Restore admin user", () -> {
            Select userSelect = new Select(findById("sel-user"));
            userSelect.selectByValue("admin");
            Select userSettingsAcl = new Select(findById("sel-user-settings-acl"));
            userSettingsAcl.selectByValue(SecurityRoles.USER_SETTINGS_READ_WRITE);
            clickOnElement("btn-confirm-save-user");
            waitForModalToShow("User already exists");
            confirmModalWith("Yes");
            waitForModalToHide("User already exists");
            waitForAjaxToComplete();
        });
    }
}
