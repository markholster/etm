package com.jecstar.etm.gui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public abstract class AbstractIntegrationTest {

    protected final String httpHost = "http://127.0.0.1:8080";
    protected WebDriver driver;

    @BeforeEach
    public void setup() {
//		this.driver = new HtmlUnitDriver(true);
//		System.setProperty("webdriver.chrome.driver", "./drivers/chromedriver");
//		this.driver = new ChromeDriver();

        System.setProperty("webdriver.gecko.driver", new File("./drivers/geckodriver-v0.19.1-linux64").getAbsolutePath());
        this.driver = new FirefoxDriver();
    }

    @AfterEach
    public void tearDown() {
        if (this.driver != null) {
            this.driver.quit();
            this.driver = null;
        }
    }

    protected void getSecurePage(String url, ExpectedCondition<?> condition) {
        getSecurePage(url, condition, -1);
    }

    protected void getSecurePage(String url, ExpectedCondition<?> condition, int waitAfterPageLoad) {
        this.driver.navigate().to(url);
        try {
            this.driver.findElement(By.id("j_username")).sendKeys("admin");
            this.driver.findElement(By.id("j_password")).sendKeys("Welkom01");
            this.driver.findElement(By.className("btn")).submit();
            if (condition != null) {
                waitFor(condition);
            }
        } catch (NoSuchElementException e) {
        }
        if (waitAfterPageLoad >= 0) {
            try {
                Thread.sleep(waitAfterPageLoad);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    protected void waitFor(ExpectedCondition<?> condition) {
        new WebDriverWait(this.driver, 10).until(condition);
    }

    protected void waitForShow(String elementId) {
        waitForShow(elementId, true);
    }

    protected void waitForShow(String elementId, boolean ignoreOpacity) {
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

    protected WebElement findById(String id) {
        return this.driver.findElement(By.id(id));
    }

    protected void setTextToElement(WebElement element, String text) {
        element.clear();
        element.sendKeys(text);
        waitFor(ExpectedConditions.attributeContains(element, "value", text));
    }

    protected boolean sendEventToEtm(String type, String data) throws IOException {
        HttpURLConnection con = null;
        DataOutputStream stream = null;
        BufferedReader in = null;
        try {
            URL url = new URL(this.httpHost + "/rest/processor/event/");
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
            return "{ \"status\": \"acknowledged\" }".equals(response.toString().trim());
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
