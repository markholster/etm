package com.jecstar.etm.gui.rest;

import com.jecstar.etm.server.core.domain.QueryOccurrence;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.joda.time.DateTimeZone;

import java.util.Objects;

class FilterQuery {

    private final QueryOccurrence queryOccurrence;

    private final QueryStringQueryBuilder query;

    //    FilterQuery(QueryOccurrence queryOccurrence, QueryBuilder query) {
    FilterQuery(QueryOccurrence queryOccurrence, QueryStringQueryBuilder query) {
        this.queryOccurrence = queryOccurrence;
        this.query = query;
    }

    public QueryOccurrence getQueryOccurrence() {
        return this.queryOccurrence;
    }

    public QueryBuilder getQuery() {
        return this.query;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FilterQuery) {
            FilterQuery other = (FilterQuery) obj;

            // Work around for bug in Elasticsearch. See FilterQueryTest.java
            DateTimeZone timeZone = query.timeZone();
            DateTimeZone otherTimeZone = other.query.timeZone();
            if (timeZone != null || otherTimeZone != null) {
                query.timeZone((DateTimeZone) null);
                other.query.timeZone((DateTimeZone) null);
                boolean result = Objects.equals(this.queryOccurrence, other.queryOccurrence) &&
                        Objects.equals(this.query, other.query) &&
                        (timeZone == null ? otherTimeZone == null : otherTimeZone != null) &&
                        Objects.equals(timeZone.getID(), otherTimeZone.getID());
                query.timeZone(timeZone);
                other.query.timeZone(otherTimeZone);
                return result;

            } else {
                return Objects.equals(this.queryOccurrence, other.queryOccurrence) && Objects.equals(this.query, other.query);
            }

        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.queryOccurrence, this.query);
    }
}
