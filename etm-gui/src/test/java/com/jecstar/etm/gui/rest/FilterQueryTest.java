package com.jecstar.etm.gui.rest;

import com.jecstar.etm.server.core.domain.QueryOccurrence;
import com.jecstar.etm.server.core.domain.principal.FilterQuery;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for the <code>FilterQuery</code> class.
 */
public class FilterQueryTest {

    /**
     * There's a bug in Elasticsearch which causes 2 QueryStringQueryBuilder to always equal when a timezone is added.
     * This testcase will fail once the bug is fixed. When that's the case the FilterQuery#query must be reverted to QueryBuilder.
     */
    @Test
    public void testCompare() {
        QueryStringQueryBuilder qb1 = new QueryStringQueryBuilder("This is a query")
                .allowLeadingWildcard(true)
                .analyzeWildcard(true)
                .timeZone("Europe/Amsterdam");
        QueryStringQueryBuilder qb2 = new QueryStringQueryBuilder("This is a different query")
                .allowLeadingWildcard(true)
                .analyzeWildcard(true)
                .timeZone("Europe/Amsterdam");
        // This should actually be false because the query is different. This assertion test the presence of an
        // elasticsearch bug and hence the need for a work around in our code.
        assertTrue(qb1.equals(qb2));
    }

    @Test
    public void testWorkAround() {
        QueryStringQueryBuilder qb1 = new QueryStringQueryBuilder("This is a query")
                .allowLeadingWildcard(true)
                .analyzeWildcard(true)
                .timeZone("Europe/Amsterdam");
        QueryStringQueryBuilder qb2 = new QueryStringQueryBuilder("This is a different query")
                .allowLeadingWildcard(true)
                .analyzeWildcard(true)
                .timeZone("Europe/Amsterdam");

        FilterQuery fq1 = new FilterQuery(QueryOccurrence.MUST, qb1);
        FilterQuery fq2 = new FilterQuery(QueryOccurrence.MUST, qb2);
        assertFalse(fq1.equals(fq2));

        // Test with qb1 and a null timezone
        qb1.timeZone((DateTimeZone) null);
        qb2.timeZone("Europe/Amsterdam");
        assertFalse(fq1.equals(fq2));

        // Test with qb2 and a null timezone
        qb1.timeZone("Europe/Amsterdam");
        qb2.timeZone((DateTimeZone) null);
        assertFalse(fq1.equals(fq2));

        // Test with both zero
        qb1.timeZone((DateTimeZone) null);
        qb2.timeZone((DateTimeZone) null);
        assertFalse(fq1.equals(fq2));

        // Now test for equality
        qb1 = new QueryStringQueryBuilder("This is a query")
                .allowLeadingWildcard(true)
                .analyzeWildcard(true)
                .timeZone("Europe/Amsterdam");
        qb2 = new QueryStringQueryBuilder("This is a query")
                .allowLeadingWildcard(true)
                .analyzeWildcard(true)
                .timeZone("Europe/Amsterdam");

        fq1 = new FilterQuery(QueryOccurrence.MUST, qb1);
        fq2 = new FilterQuery(QueryOccurrence.MUST, qb2);
        assertTrue(fq1.equals(fq2));

        // Test with qb1 and a null timezone
        qb1.timeZone((DateTimeZone) null);
        qb2.timeZone("Europe/Amsterdam");
        assertFalse(fq1.equals(fq2));

        // Test with qb2 and a null timezone
        qb1.timeZone("Europe/Amsterdam");
        qb2.timeZone((DateTimeZone) null);
        assertFalse(fq1.equals(fq2));

        // Test with both zero
        qb1.timeZone((DateTimeZone) null);
        qb2.timeZone((DateTimeZone) null);
        assertTrue(fq1.equals(fq2));
    }
}
