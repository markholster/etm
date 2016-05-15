package com.jecstar.etm.launcher.gui;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

public abstract class AbstractIntegrationTest {
	
	protected WebDriver driver;
	
	@Before
	public void setup() {
//		this.driver = new HtmlUnitDriver(true);
		File profileDir = new File("profiles/firefox");
		FirefoxProfile profile = new FirefoxProfile(profileDir);
		this.driver = new FirefoxDriver(profile);
	}
	
	@After
	public void tearDown() {
		if (this.driver != null) {
			this.driver.close();
		}
	}
	
	protected void getSecurePage(String url) {
	    this.driver.get(url);
	    try {
		    this.driver.findElement(By.id("j_username")).sendKeys("admin");     
		    this.driver.findElement(By.id("j_password")).sendKeys("password");     
		    this.driver.findElement(By.className("btn")).submit();	    	
	    } catch (NoSuchElementException e) {}
	}
	
	protected void waitForShow(String elementId) {
	    new WebDriverWait(this.driver, 10).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
            	try {
            		return d.findElement(By.id(elementId)).isDisplayed();
            	} catch (NoSuchElementException e) {}
            	return false;
            }
        });
	}
	
	protected void waitForHide(String elementId) {
	    new WebDriverWait(this.driver, 10).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
            	try {
            		return !d.findElement(By.id(elementId)).isDisplayed();
            	} catch (NoSuchElementException e) {}
            	return true;
            }
        });
	}

}
