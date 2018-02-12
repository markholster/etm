package com.jecstar.etm.gui.search;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


/**
 * Class testing the search functionality of ETM.
 *
 * @author Mark Holster
 */
public class QueryTest extends AbstractSearchIntegrationTest {

    @Test
    public void testSearch() {
        final String query = "endpoints.writing_endpoint_handler.application.name: \"Enterprise Telemetry Monitor\"";
        final String nameColumnName = "Name";
        final String newColumnName = "New column";
        final String newColumnField = "log_level";

        getSecurePage(this.httpHost + "/gui/search/index.html", null, 1000);

        // Only search for log events.
        ((JavascriptExecutor) driver).executeScript("arguments[0].removeAttribute('checked','checked')", findById("check-type-business"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].removeAttribute('checked','checked')", findById("check-type-http"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].removeAttribute('checked','checked')", findById("check-type-messaging"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].removeAttribute('checked','checked')", findById("check-type-sql"));

        // Search for log event we of this application.
        findById("query-string").sendKeys(query);
        findById("btn-search").click();
        waitForShow("search_result_table");

        // First check if the "Query history" list is updated with our query. It should be the first link
        waitFor(d -> {
            WebElement historyLink = d.findElement(By.id("list-search-history-links")).findElement(By.xpath("./*[1]/*[1]"));
            return historyLink != null && historyLink.getAttribute("title").equals(query);
        });


        // Now, lets sort on the name.
        WebElement nameColumn = findById("search_result_table").findElement(By.xpath("./thead/tr/th[text()='" + nameColumnName + "']"));
        nameColumn.click();
        // Select the header that is sorted.
        waitFor(d -> d.findElement(By.id("search_result_table")).findElement(By.xpath("./thead/tr/th[@class='headerSortDesc' and text()='" + nameColumnName + "']")) != null);

        // Extend the search table with another column.
        // By default there should be 2 columns in the result table.
        assertEquals(2, findById("search_result_table").findElements(By.xpath("./thead/tr/th")).size());
        // Test if the table settings screen is hidden.
        assertFalse(findById("modal-table-settings").isDisplayed());
        // Click on the table settings icon
        findById("link-edit-table").click();
        waitForShow("modal-table-settings");
        // Lets add a new column.
        findById("link-add-result-row").click();
        // Select the fourth child. first child is the header, second is the timestamp column, third is the name column and the fourth is our new column.
        WebElement newRowElement = findById("table-settings-columns").findElement(By.xpath("./*[4]"));
        // Set the column name and field
        newRowElement.findElement(By.xpath("./*[1]/*[1]")).sendKeys(newColumnName);
        newRowElement.findElement(By.xpath("./*[2]/*[1]")).sendKeys(newColumnField);
        // Save the new layout.
        findById("btn-apply-table-settings").click();
        waitForHide("modal-table-settings");
        // Check if 3 columns are shown in the result table.
        assertEquals(3, findById("search_result_table").findElements(By.xpath("./thead/tr/th")).size());
    }
}
