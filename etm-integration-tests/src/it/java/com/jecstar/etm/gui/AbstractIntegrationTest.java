package com.jecstar.etm.gui;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.function.Predicate;

import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

public abstract class AbstractIntegrationTest {
	
	protected final String httpHost = "http://127.0.0.1:8080";
	protected WebDriver driver;
	
	@Before
	public void setup() {
//		this.driver = new HtmlUnitDriver(true);
//		System.setProperty("webdriver.chrome.driver", "./drivers/chromedriver");
//		this.driver = new ChromeDriver();

		System.setProperty("webdriver.gecko.driver", "./drivers/geckodriver-0.11.1-linux64");
		DesiredCapabilities capabilities = DesiredCapabilities.firefox();
		capabilities.setJavascriptEnabled(true);
		this.driver = new FirefoxDriver(capabilities);
	}
	
	@After
	public void tearDown() {
		if (this.driver != null) {
			this.driver.close();
			this.driver.quit();
			this.driver = null;
		}
	}
	
	protected void getSecurePage(String url, String idOfElementToWaitFor) {
	    this.driver.navigate().to(url);
	    try {
		    this.driver.findElement(By.id("j_username")).sendKeys("admin");     
		    this.driver.findElement(By.id("j_password")).sendKeys("password");     
		    this.driver.findElement(By.className("btn")).submit();
		    waitForShow(idOfElementToWaitFor);
	    } catch (NoSuchElementException e) {}
	}
	
	protected void waitFor(Predicate<WebDriver> predicate) {
	    new WebDriverWait(this.driver, 10).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
            	return predicate.test(d);
            }
        });
	}
	
	protected void waitForShow(String elementId) {
		waitFor(d -> d.findElement(By.id(elementId)).isDisplayed());
	}
	
	protected void waitForHide(String elementId) {
		waitFor(d -> !d.findElement(By.id(elementId)).isDisplayed());
	}
	
	/**
	 * Wait for an event id to show up. This can be useful if an event is added,
	 * but still not present at the database. This method is filling in the
	 * search field and polling for the result to show up.
	 * 
	 * @param eventId
	 *            The event to search for,
	 * @return 
	 */
	protected WebElement waitForSearchResult(String eventId) {
		this.driver.findElement(By.id("query-string")).clear();
		this.driver.findElement(By.id("query-string")).sendKeys("id: " + eventId);
		this.driver.findElement(By.id("btn-search")).click();
		long startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() - startTime < 10000) {
			try {
				return this.driver.findElement(By.id(eventId));
			} catch (NoSuchElementException e) {
				try {
					Thread.sleep(750);
					this.driver.findElement(By.id("btn-search")).click();
				} catch (InterruptedException e1) {
					Thread.currentThread().interrupt();
				}
			}
		}
		throw new NoSuchElementException("Element with id '" + eventId + "' not found.");
		
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
