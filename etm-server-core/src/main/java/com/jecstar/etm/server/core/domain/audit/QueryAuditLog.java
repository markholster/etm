package com.jecstar.etm.server.core.domain.audit;


/**
 * The audit log that occurs when a user executes a query.
 *
 * @author Mark Holster
 */
public class QueryAuditLog extends AuditLog<QueryAuditLog> {

    /**
     * Holds the query the user executed.
     */
    public String userQuery;

    /**
     * Holds the actual query that is executed.
     */
    public String executedQuery;

    /**
     * The number of matched results.
     */
    public long numberOfResults;

    /**
     * The time in milliseconds the query took.
     */
    public long queryTime;

}
