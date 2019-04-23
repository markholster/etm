package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.PipelineAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;

public class SearchRequestBuilder extends AbstractActionRequestBuilder<SearchRequest> {

    private final SearchRequest request = new SearchRequest();
    private final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

    public SearchRequestBuilder setIndices(String... indices) {
        this.request.indices(indices);
        return this;
    }

    public SearchRequestBuilder setScroll(TimeValue timeValue) {
        this.request.scroll(timeValue);
        return this;
    }

    public SearchRequestBuilder setQuery(QueryBuilder query) {
        this.searchSourceBuilder.query(query);
        return this;
    }

    public SearchRequestBuilder setSort(String name, SortOrder order) {
        this.searchSourceBuilder.sort(name, order);
        return this;
    }

    public SearchRequestBuilder setSort(FieldSortBuilder fieldSortBuilder) {
        this.searchSourceBuilder.sort(fieldSortBuilder.getFieldName(), fieldSortBuilder.order());
        return this;
    }

    public SearchRequestBuilder setFetchSource(boolean fetch) {
        this.searchSourceBuilder.fetchSource(fetch);
        return this;
    }

    public SearchRequestBuilder setFetchSource(String include, String exclude) {
        this.searchSourceBuilder.fetchSource(include, exclude);
        return this;
    }

    public SearchRequestBuilder setFetchSource(String[] includes, String[] excludes) {
        this.searchSourceBuilder.fetchSource(includes, excludes);
        return this;
    }

    public SearchRequestBuilder setSize(int size) {
        this.searchSourceBuilder.size(size);
        return this;
    }

    public SearchRequestBuilder setFrom(int from) {
        this.searchSourceBuilder.from(from);
        return this;
    }

    public SearchRequestBuilder setTimeout(TimeValue timeValue) {
        this.searchSourceBuilder.timeout(timeValue);
        return this;
    }

    public SearchRequestBuilder addAggregation(AggregationBuilder aggregationBuilder) {
        this.searchSourceBuilder.aggregation(aggregationBuilder);
        return this;
    }

    public SearchRequestBuilder addAggregation(PipelineAggregationBuilder pipelineAggregationBuilder) {
        this.searchSourceBuilder.aggregation(pipelineAggregationBuilder);
        return this;
    }

    @Override
    public SearchRequest build() {
        return this.request.source(this.searchSourceBuilder);
    }
}
