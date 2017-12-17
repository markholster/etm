package com.jecstar.etm.gui.search;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.junit.Assert.*;

/**
 * Class testing the usage of the search templates in the GUI.
 * 
 * @author Mark Holster
 */
public class SearchTemplateTest extends AbstractSearchIntegrationTest {

	@Test
	public void testTemplateManagement() {
		final String templateName = "Integration-test";
		final String templateQuery = "*";
		final String updatedTemplateQuery = "This is a test";
		
		// Go to the search page
		getSecurePage(this.httpHost + "/gui/search/index.html", c -> ExpectedConditions.visibilityOf(findById("query-string")), 1000);

        // Check if certain fields are disabled.
	    assertFalse("Search button is enabled when search string is not provided", findById("btn-search").isEnabled());
	    assertFalse("Template name input is enabled when search string is not provided", findById("template-name").isEnabled());
	    assertFalse("Template save button is enabled when search string is not provided", findById("btn-save-template").isEnabled());
	    
	    // Fill in the query field.
        setTextToElement(findById("query-string"), templateQuery);
	    // Make sure the search button and template name field are enabled now.
		waitFor(ExpectedConditions.elementToBeClickable(findById("btn-search")));
        waitFor(ExpectedConditions.elementToBeClickable(findById("template-name")));
	    // Make sure the template save button is still disabled because the template name is still empty.
	    assertFalse("Template save button is enabled when template name is not provided", findById("btn-save-template").isEnabled());
	    
	    // Execute the query and wait for the result table. This table is created with Javascript, so not present before the query is executed.
	    findById("btn-search").click();
	    waitForShow("search_result_table");
	    
	    // Fill the name of the search template.
	    setTextToElement(findById("template-name"), templateName);
	    // Make sure the template save button is enabled now.
	    assertTrue("Template save button is enabled when template name is not provided", findById("btn-save-template").isEnabled());
	    // Save the template and make sure the template is available afterwards.
	    findById("btn-save-template").click();
	    waitFor(ExpectedConditions.presenceOfNestedElementLocatedBy(By.id("list-template-links"), By.xpath("./li/a[text()='" + templateName + "']")));
	    
	    // Now let's check the template is working. First we need to change the query field contents
        setTextToElement(findById("query-string"),"This value should be changed with the value of the template");
	    // Select the template.
	    findById("list-template-links").findElement(By.xpath("./li/a[text()='" + templateName + "']")).click();
        // Now make sure the query field contains the template values.
	    assertEquals(templateQuery, findById("query-string").getAttribute("value"));

	    // Check if updating the template works like expected.
	    assertFalse(findById("modal-template-overwrite").isDisplayed());
	    // We update the query field with a new query
        setTextToElement(findById("query-string"), updatedTemplateQuery);
	    // Set the template name to the existing template
        setTextToElement(findById("template-name"), templateName);
	    // And hit the save button
	    findById("btn-save-template").click();
	    // The confirmation window should be shown
	    waitForShow("modal-template-overwrite", false);
	    // Confirm the overwrite and check the result
        findById("btn-overwrite-template").click();
	    // The confirmation window should be hidden again.
	    waitForHide("modal-template-overwrite");
	    // And if we select the template, the updated query should be used.
	    findById("list-template-links").findElement(By.xpath("./li/a[text()='" + templateName + "']")).click();
	    assertEquals(updatedTemplateQuery, findById("query-string").getAttribute("value"));
	    
	    // Finally, remove the template.
	    assertFalse(findById("modal-template-remove").isDisplayed());
	    findById("list-template-links").findElement(By.xpath("./li[a[text()='" + templateName + "']]/a[@class='fa fa-times pull-right text-danger']")).click();
	    // The confirmation window should be shown
	    waitForShow("modal-template-remove", false);
	    // Confirm the removal and check the result
	    findById("btn-remove-template").click();
	    // The confirmation window should be hidden again.
	    waitForHide("modal-template-remove");
	    // The template should be removed.
	    try {
	    	findById("list-template-links").findElement(By.xpath("./li/a[text()='" + templateName + "']"));
	    	fail("Template '" + templateName + "' not removed.");
	    } catch (NoSuchElementException e) {}
	    
	}
	
}
