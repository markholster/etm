package com.jecstar.etm.gui;

import org.junit.After;
import org.junit.Before;
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
	
	@Before
	public void setup() {
//		this.driver = new HtmlUnitDriver(true);
//		System.setProperty("webdriver.chrome.driver", "./drivers/chromedriver");
//		this.driver = new ChromeDriver();

		System.setProperty("webdriver.gecko.driver", new File("./drivers/geckodriver-v0.19.0-linux64").getAbsolutePath());
		this.driver = new FirefoxDriver();
	}
	
	@After
	public void tearDown() {
		if (this.driver != null) {
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
	
	protected void waitFor(ExpectedCondition<?> condition) {
	    new WebDriverWait(this.driver, 10).until(condition);
	}
	
	protected void waitForShow(String elementId) {
		waitFor(ExpectedConditions.visibilityOfElementLocated(By.id(elementId)));
	}
	
	protected void waitForHide(String elementId) {
		waitFor(ExpectedConditions.invisibilityOfElementLocated(By.id(elementId)));
	}

	protected WebElement findById(String id) {
		return this.driver.findElement(By.id(id));
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
