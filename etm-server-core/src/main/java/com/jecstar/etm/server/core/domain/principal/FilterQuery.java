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
