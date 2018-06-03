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
     * The time in milliseconds the query took.
     */
    @JsonField("query_time")
    public long queryTime;

}
