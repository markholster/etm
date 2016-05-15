package com.jecstar.etm.launcher.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class QueryTest extends AbstractIntegrationTest {
	
	@Test
	public void testSearch() {
		final String query = "endpoints.writing_endpoint_handler.application.name: \"Enterprise Telemetry Monitor\"";
		final String nameColumnName = "Name";
		final String newColumnName = "New column";
		final String newColumnField = "log_level";
		
		getSecurePage("http://127.0.0.1:8080/gui/search/index.html");
		
		// Only search for log events.
	    this.driver.findElement(By.id("check-type-business")).click();
	    this.driver.findElement(By.id("check-type-http")).click();
	    this.driver.findElement(By.id("check-type-messaging")).click();
	    this.driver.findElement(By.id("check-type-sql")).click();

	    // Search for log event we of this application.
	    this.driver.findElement(By.id("query-string")).sendKeys(query);
	    this.driver.findElement(By.id("btn-search")).click();
	    waitForShow("search_result_table");

	    // First check if the "Recent queries" list is updated with our query. It should be the first link
	    WebElement recentLink = this.driver.findElement(By.id("list-recent-queries-links")).findElement(By.xpath("./*[1]/*[1]"));
	    assertEquals(query, recentLink.getAttribute("title"));
	    
	    // Now, lets sort on the name.
	    WebElement nameColumn = this.driver.findElement(By.id("search_result_table")).findElement(By.xpath("./thead/tr/th[text()='" + nameColumnName + "']"));
	    nameColumn.click();
	    // Select the header that is sorted.
	    WebElement sortHeader = this.driver.findElement(By.id("search_result_table")).findElement(By.xpath("./thead/tr/th[@class='headerSortDesc']"));
	    assertEquals(nameColumnName, sortHeader.getText());
	    
	    // Extend the search table with another column.
	    // By default there should be 2 columns in the result table.
	    assertEquals(2, this.driver.findElement(By.id("search_result_table")).findElements(By.xpath("./thead/tr/th")).size());
	    // Test if the table settings screen is hidden.
	    assertFalse(this.driver.findElement(By.id("modal-table-settings")).isDisplayed());
	    // Click on the table settings icon
	    this.driver.findElement(By.id("link-edit-table")).click();
	    waitForShow("modal-table-settings");
	    // Lets add a new column.
	    this.driver.findElement(By.id("link-add-result-row")).click();
	    // Select the fourth child. first child is the header, second is the timestamp column, third is the name column and the fourth is our new column.
	    WebElement newRowElement = this.driver.findElement(By.id("table-settings-columns")).findElement(By.xpath("./*[4]"));
	    // Set the column name and field
	    newRowElement.findElement(By.xpath("./*[1]/*[1]")).sendKeys(newColumnName);
	    newRowElement.findElement(By.xpath("./*[2]/*[1]")).sendKeys(newColumnField);
	    // Save the new layout.
	    this.driver.findElement(By.id("btn-apply-table-settings")).click();
	    waitForHide("modal-table-settings");
	    // Check if 3 columns are shown in the result table.
	    assertEquals(3, this.driver.findElement(By.id("search_result_table")).findElements(By.xpath("./thead/tr/th")).size());
	}
}
