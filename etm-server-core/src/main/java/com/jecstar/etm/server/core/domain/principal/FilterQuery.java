package com.jecstar.etm.server.core.domain.principal;

import com.jecstar.etm.server.core.domain.QueryOccurrence;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;

import java.util.Objects;

public class FilterQuery {

    private final QueryOccurrence queryOccurrence;

    private final QueryStringQueryBuilder query;

    public FilterQuery(QueryOccurrence queryOccurrence, QueryStringQueryBuilder query) {
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
            return Objects.equals(this.queryOccurrence, other.queryOccurrence) && Objects.equals(this.query, other.query);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.queryOccurrence, this.query);
    }
}
