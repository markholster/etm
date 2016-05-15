package com.jecstar.etm.launcher.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Class testing the usage of the search templates in the GUI.
 * 
 * @author Mark Holster
 */
public class SearchTemplateTest extends AbstractIntegrationTest{

	@Test
	public void testTemplateManagement() {
		final String templateName = "Integration-test";
		final String templateQuery = "*";
		final String updatedTemplateQuery = "This is a test";
		
		// Go to the search page
		getSecurePage("http://127.0.0.1:8080/gui/search/index.html");
		
	    // Check if certain fields are disabled.
	    assertFalse("Search button is enabled when search string is not provided", this.driver.findElement(By.id("btn-search")).isEnabled());
	    assertFalse("Template name input is enabled when search string is not provided", this.driver.findElement(By.id("template-name")).isEnabled());
	    assertFalse("Template save button is enabled when search string is not provided", this.driver.findElement(By.id("btn-save-template")).isEnabled());
	    
	    // Fill in the query field.
	    this.driver.findElement(By.id("query-string")).sendKeys(templateQuery);
	    // Make sure the search button and template name field are enabled now.
	    assertTrue("Search button is not enabled when search string is provided", this.driver.findElement(By.id("btn-search")).isEnabled());
	    assertTrue("Template name input is not enabled when search string is provided", this.driver.findElement(By.id("template-name")).isEnabled());
	    // Make sure the template save button is still disabled because the template name is still empty.
	    assertFalse("Template save button is enabled when template name is not provided", this.driver.findElement(By.id("btn-save-template")).isEnabled());
	    
	    // Execute the query and wait for the result table. This table is created with Javascript, so not present before the query is executed.
	    this.driver.findElement(By.id("btn-search")).click();
	    assertNotNull("Result table not shown", this.driver.findElement(By.id("search_result_table")));
	    waitForShow("search_result_table");
	    
	    // Fill the name of the search template.
	    this.driver.findElement(By.id("template-name")).sendKeys(templateName);
	    // Make sure the template save button is enabled now.
	    assertTrue("Template save button is enabled when template name is not provided", this.driver.findElement(By.id("btn-save-template")).isEnabled());
	    // Save the template and make sure the template is available afterwards.
	    this.driver.findElement(By.id("btn-save-template")).click();
	    new WebDriverWait(this.driver, 10).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
            	try {
            		d.findElement(By.id("list-template-links")).findElement(By.xpath("./li/a[text()='" + templateName + "']"));
            		return true;
            	} catch (NoSuchElementException e) {}
            	return false;
            }
        });
	    
	    // Now let's check the template is working. First we need to change the query field contents
	    this.driver.findElement(By.id("query-string")).sendKeys("This value should be changed with the value of the template");
	    // Select the template.
	    this.driver.findElement(By.id("list-template-links")).findElement(By.xpath("./li/a[text()='" + templateName + "']")).click();
	    // Now make sure the query field contains the template values.
	    assertEquals(templateQuery, this.driver.findElement(By.id("query-string")).getAttribute("value"));
	    
	    
	    // Check if updating the template works like expected.
	    assertFalse(this.driver.findElement(By.id("modal-template-overwrite")).isDisplayed());
	    // We update the query field with a new query
	    this.driver.findElement(By.id("query-string")).clear();
	    this.driver.findElement(By.id("query-string")).sendKeys(updatedTemplateQuery);
	    // Set the template name to the existing template
	    this.driver.findElement(By.id("template-name")).clear();
	    this.driver.findElement(By.id("template-name")).sendKeys(templateName);
	    // And hit the save button
	    this.driver.findElement(By.id("btn-save-template")).click();
	    // The confirmation window should be shown
	    waitForShow("modal-template-overwrite");
	    // Confirm the overwrite and check the result
	    this.driver.findElement(By.id("btn-overwrite-template")).click();
	    // The confirmation window should be hidden again.
	    waitForHide("modal-template-overwrite");
	    // And if we select the template, the updated query should be used.
	    this.driver.findElement(By.id("list-template-links")).findElement(By.xpath("./li/a[text()='" + templateName + "']")).click();
	    assertEquals(updatedTemplateQuery, this.driver.findElement(By.id("query-string")).getAttribute("value"));
	    
	    // Finally, remove the template.
	    assertFalse(this.driver.findElement(By.id("modal-template-remove")).isDisplayed());
	    this.driver.findElement(By.id("list-template-links")).findElement(By.xpath("./li[a[text()='" + templateName + "']]/a[@class='fa fa-times pull-right text-danger']")).click();
	    // The confirmation window should be shown
	    waitForShow("modal-template-remove");
	    // Confirm the removal and check the result
	    this.driver.findElement(By.id("btn-remove-template")).click();
	    // The confirmation window should be hidden again.
	    waitForHide("modal-template-remove");
	    // The template should be removed.
	    try {
	    	this.driver.findElement(By.id("list-template-links")).findElement(By.xpath("./li/a[text()='" + templateName + "']"));
	    	fail("Template '" + templateName + "' not removed.");
	    } catch (NoSuchElementException e) {}
	    
	}
	
}
