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

package com.jecstar.etm.server.core.domain.audit;


import com.jecstar.etm.server.core.converter.JsonField;

/**
 * The audit log that occurs when a user executes a query.
 *
 * @author Mark Holster
 */
public class QueryAuditLog extends AuditLog {

    /**
     * Holds the query the user executed.
     */
    @JsonField("user_query")
    public String userQuery;

    /**
     * Holds the actual query that is executed.
     */
    @JsonField("executed_query")
    public String executedQuery;

    /**
     * The number of matched results.
     */
    @JsonField("number_of_results")
    public long numberOfResults;

    /**
     * The relation of number of results. Can be EQ or GTE.
     */
    @JsonField("number_of_results_relation")
    public String numberOfResultsRelation;

    /**
     * The time in milliseconds the query took.
     */
    @JsonField("query_time")
    public long queryTime;

}
