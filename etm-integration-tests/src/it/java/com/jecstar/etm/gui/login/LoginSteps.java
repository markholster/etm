package com.jecstar.etm.gui.login;

import com.jecstar.etm.gui.AbstractSteps;
import org.openqa.selenium.WebElement;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unused")
public class LoginSteps extends AbstractSteps {

    public LoginSteps() {
        Then("The login page should be visible", () -> {
            waitForAjaxToComplete();
            WebElement webElement = findById("event-count");
            assertNotNull(webElement);
            assertTrue(Long.valueOf(webElement.getText().replaceAll(",", "").replaceAll("\\.", "")) > 0);
        });
    }
}
