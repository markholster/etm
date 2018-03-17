package com.jecstar.etm.gui;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import cucumber.api.java8.En;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.*;

public abstract class AbstractSteps implements En {

    protected static final String host = "http://127.0.0.1:8080";

    protected WebDriver driver;

    protected String username;

    protected AbstractSteps() {
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit.DefaultCssErrorHandler").setLevel(Level.OFF);
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit.javascript.StrictErrorReporter").setLevel(Level.OFF);

        // Teardown the Selenium driver.
        After(() -> {
            waitForAjaxToComplete();
            this.driver.navigate().to(host + "/gui/logout?source=./");
            this.driver.quit();
        });

        //Login to ETM. This should always be the first step of a testcase.
        Given("The user logs in to ETM as (.*) with password (.*) using (.*)", (String username, String password, String browser) -> {
            this.username = username;
            if ("firefox".equalsIgnoreCase(browser)) {
                System.setProperty("webdriver.gecko.driver", new File("./drivers/geckodriver").getAbsolutePath());
                this.driver = new FirefoxDriver();
            } else if ("edge".equalsIgnoreCase(browser)) {
                this.driver = new HtmlUnitDriver(BrowserVersion.EDGE, true);
            } else if ("ie".equalsIgnoreCase(browser)) {
                this.driver = new HtmlUnitDriver(BrowserVersion.INTERNET_EXPLORER, true);
            } else if ("chrome".equalsIgnoreCase(browser)) {
                System.setProperty("webdriver.chrome.driver", "./drivers/chromedriver");
                this.driver = new ChromeDriver();
//                this.driver = new HtmlUnitDriver(BrowserVersion.CHROME, true);
            } else {
                this.driver = new HtmlUnitDriver(BrowserVersion.getDefault(), true);
            }
            this.driver.navigate().to(host + "/gui/");
            try {
                findById("j_username").sendKeys(username);
                findById("j_password").sendKeys(password);
                find(By.className("btn")).submit();
                waitFor(ExpectedConditions.visibilityOfElementLocated(By.id("etm-header")));
            } catch (NoSuchElementException e) {
                fail(e);
            }
        });

        // Browse to a page.
        And("The user browses to (.*)", (String page) -> {
            this.driver.navigate().to(host + page);
            waitForAjaxToComplete();
        });

        // Wait for a modal to be visible.
        Then("The \"(.*)\" modal should be shown", this::waitForModalToShow);

        When("The user confirms the modal with \"(.*)\"", this::confirmModalWith);

        Then("The element with id \"(.*)\" is enabled", (String elementId) -> assertTrue(findById(elementId).isEnabled()));
        Then("The element with id \"(.*)\" is disabled", (String elementId) -> assertFalse(findById(elementId).isEnabled()));

        Then("The user logs out", () -> {
            this.driver.navigate().to(host + "/gui/logout?source=./");
            this.driver.close();
        });
    }

    protected WebElement findById(String id) {
        return find(By.id(id));
    }

    protected WebElement find(By by) {
        return this.driver.findElement(by);
    }

    protected void waitFor(ExpectedCondition<?> condition) {
        waitFor(condition, 10);
    }

    protected void waitFor(ExpectedCondition<?> condition, int timeOutInSeconds) {
        new WebDriverWait(this.driver, timeOutInSeconds).until(condition);
    }

    protected void waitForShow(final String elementId) {
        waitForShow(elementId, true);
    }

    protected void waitForShow(final String elementId, final boolean ignoreOpacity) {
        waitFor(ExpectedConditions.visibilityOfElementLocated(By.id(elementId)));
        if (!ignoreOpacity) {
            waitFor(c -> c.findElement(By.id(elementId)).getCssValue("opacity").equals("1"));
        }
    }

    protected void waitForHide(String elementId) {
        waitForHide(elementId, true);
    }

    protected void waitForHide(String elementId, boolean ignoreOpacity) {
        waitFor(ExpectedConditions.invisibilityOfElementLocated(By.id(elementId)));
        if (!ignoreOpacity) {
            waitFor(c -> c.findElement(By.id(elementId)).getCssValue("opacity").equals("0"));
        }
    }

    protected void setTextToElement(WebElement element, String text) {
        element.clear();
        element.sendKeys(text);
        waitFor(ExpectedConditions.attributeContains(element, "value", text));
    }

    protected void waitForAjaxToComplete() {
        waitFor(c -> ((JavascriptExecutor) this.driver).executeScript("return jQuery.active == 0"));
    }

    protected void waitForModalToShow(String modalTitle) {
        WebElement titleElement = find(By.xpath("//*[@class='modal-title' and text()='" + modalTitle + "']"));
        assertNotNull(titleElement);
        WebElement modalElement = titleElement.findElement(By.xpath("ancestor::*[contains(@class, 'modal-dialog')]"));
        waitFor(ExpectedConditions.visibilityOf(modalElement));
    }

    protected void waitForModalToHide(String modalTitle) {
        WebElement titleElement = find(By.xpath("//*[@class='modal-title' and text()='" + modalTitle + "']"));
        assertNotNull(titleElement);
        WebElement modalElement = titleElement.findElement(By.xpath("ancestor::*[contains(@class, 'modal-dialog')]"));
        waitFor(ExpectedConditions.invisibilityOf(modalElement));
        try {
            waitFor(ExpectedConditions.invisibilityOf(find(By.xpath("//*[contains(@class, 'modal-backdrop')]"))));
        } catch (NoSuchElementException e) {
        }
    }

    protected void clickOnElement(WebElement element) {
        waitFor(c -> element.isEnabled() && element.isDisplayed());
        waitFor(ExpectedConditions.elementToBeClickable(element));
        element.click();
    }

    protected void clickOnElement(String elementId) {
        clickOnElement(findById(elementId));
    }

    protected void confirmModalWith(String buttonName) {
        List<WebElement> buttons = this.driver.findElements(By.xpath("//div[@class='modal-footer']/button[text()='" + buttonName + "']"));
        for (WebElement button : buttons) {
            if (button.isEnabled() && button.isDisplayed()) {
                button.click();
                if (this.driver instanceof FirefoxDriver) {
                    // Hack to make the buttons of a modal work in firefox
                    button.click();
                }
                WebElement modalElement = button.findElement(By.xpath("ancestor::*[contains(@class, 'modal-dialog')]"));
                waitFor(ExpectedConditions.invisibilityOf(modalElement));
                break;
            }
        }
    }

    protected void sleepWhenChrome(long milliseconds) {
        if (this.driver instanceof ChromeDriver) {
            try {
                Thread.sleep(milliseconds);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    protected void sleepWhenFirefox(long milliseconds) {
        if (this.driver instanceof FirefoxDriver) {
            try {
                Thread.sleep(milliseconds);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    protected boolean sendEventToEtm(String type, String data) throws IOException {
        HttpURLConnection con = null;
        DataOutputStream stream = null;
        BufferedReader in = null;
        try {
            URL url = new URL(this.host + "/rest/processor/event/");
            con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(1000);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            con.setDoOutput(true);
            stream = new DataOutputStream(con.getOutputStream());
            stream.write(("{\"type\": \"" + type + "\", \"data\": " + data + "}").getBytes(Charset.forName("utf-8")));
            stream.flush();
            stream.close();

            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            response.append("");
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            con.disconnect();
            return "{ \"status\": \"acknowledged\" }" .equals(response.toString().trim());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
            if (con != null) {
                con.disconnect();
            }
        }
    }
}
