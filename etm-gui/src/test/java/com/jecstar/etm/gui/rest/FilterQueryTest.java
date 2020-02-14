/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.gui.rest;

import com.jecstar.etm.server.core.domain.QueryOccurrence;
import com.jecstar.etm.server.core.domain.principal.FilterQuery;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for the <code>FilterQuery</code> class.
 */
public class FilterQueryTest {

    /**
     * There's a bug in Elasticsearch which causes 2 QueryStringQueryBuilder to always equal when a timezone is added.
     * This testcase will fail once the bug is fixed. When that's the case the FilterQuery#query must be reverted to QueryBuilder.
     *
     * See https://github.com/elastic/elasticsearch/issues/29403 for more information. Work around no longer in place.
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
        assertFalse(qb1.equals(qb2));
    }

    /**
     * See https://github.com/elastic/elasticsearch/issues/29403, work around no longer in place. Issue is fixed in ES 6.3
     */
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
        qb1.timeZone((ZoneId) null);
        qb2.timeZone("Europe/Amsterdam");
        assertFalse(fq1.equals(fq2));

        // Test with qb2 and a null timezone
        qb1.timeZone("Europe/Amsterdam");
        qb2.timeZone((ZoneId) null);
        assertFalse(fq1.equals(fq2));

        // Test with both zero
        qb1.timeZone((ZoneId) null);
        qb2.timeZone((ZoneId) null);
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
        qb1.timeZone((ZoneId) null);
        qb2.timeZone("Europe/Amsterdam");
        assertFalse(fq1.equals(fq2));

        // Test with qb2 and a null timezone
        qb1.timeZone("Europe/Amsterdam");
        qb2.timeZone((ZoneId) null);
        assertFalse(fq1.equals(fq2));

        // Test with both zero
        qb1.timeZone((ZoneId) null);
        qb2.timeZone((ZoneId) null);
        assertTrue(fq1.equals(fq2));
    }
}
