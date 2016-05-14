package com.jecstar.etm.launcher.gui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Class testing the usage of the search templates in the GUI.
 * 
 * @author Mark Holster
 */
public class SearchTemplateTest {

	private WebDriver driver;

	@Before
	public void setup() {
		this.driver = new HtmlUnitDriver(true);
	}
	
	@After
	public void tearDown() {
		if (this.driver != null) {
			this.driver.close();
		}
	}
	
	@Test
	public void testTemplateManagement() {
	    this.driver.get("http://127.0.0.1:8080/gui/search/index.html");             
	    this.driver.findElement(By.id("j_username")).sendKeys("admin");     
	    this.driver.findElement(By.id("j_password")).sendKeys("password");     
	    this.driver.findElement(By.className("btn")).submit();
	    
	    
	    // Search button should be disabled when no query entered yet
	    assertFalse("Search button is enabled when search string is not provided", this.driver.findElement(By.id("btn-search")).isEnabled());
	    assertFalse("Template name input is enabled when search string is not provided", this.driver.findElement(By.id("template-name")).isEnabled());
	    assertFalse("Template save button is enabled when search string is not provided", this.driver.findElement(By.id("btn-save-template")).isEnabled());
	    this.driver.findElement(By.id("query-string")).sendKeys("*");
	    assertTrue("Search button is not enabled when search string is provided", this.driver.findElement(By.id("btn-search")).isEnabled());
	    assertTrue("Template name input is not enabled when search string is provided", this.driver.findElement(By.id("template-name")).isEnabled());
	    assertFalse("Template save button is enabled when template name is not provided", this.driver.findElement(By.id("btn-save-template")).isEnabled());
	    
	    this.driver.findElement(By.id("btn-search")).click();
	    new WebDriverWait(this.driver, 10).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
            	try {
            		d.findElement(By.id("search_result_table"));
            		return true;
            	} catch (NoSuchElementException e) {}
            	return false;
            }
        });
	    
	    
	    this.driver.findElement(By.id("template-name")).sendKeys("Integration-test");
	    assertTrue("Template save button is enabled when template name is not provided", this.driver.findElement(By.id("btn-save-template")).isEnabled());
	    this.driver.findElement(By.id("btn-save-template")).click();
	    new WebDriverWait(this.driver, 10).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
            	try {
            		d.findElement(By.id("list-template-links")).findElement(By.xpath("./li/a[text()='Integration-test']"));
            		return true;
            	} catch (NoSuchElementException e) {}
            	return false;
            }
        });
	    
	}
	
}
