package com.jecstar.etm.launcher.gui;

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
	public void testTemplateManagerment() {
	    this.driver.get("http://127.0.0.1:8080/gui/search/index.html");             
	    this.driver.findElement(By.id("j_username")).sendKeys("admin");     
	    this.driver.findElement(By.id("j_password")).sendKeys("password");     
	    this.driver.findElement(By.className("btn")).submit();
	    
	    this.driver.findElement(By.id("query-string")).sendKeys("*");
	    this.driver.findElement(By.id("btn-search")).click();
	    (new WebDriverWait(this.driver, 10)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
            	try {
            		d.findElement(By.id("search_result_table"));
            		return true;
            	} catch (NoSuchElementException e) {}
            	return false;
            }
        });
	    
	}
	
}
