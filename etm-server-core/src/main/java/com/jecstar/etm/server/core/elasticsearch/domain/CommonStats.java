package com.jecstar.etm.server.core.elasticsearch.domain;

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.elasticsearch.domain.converter.DocsStatsConverter;
import com.jecstar.etm.server.core.elasticsearch.domain.converter.IndexingStatsConverter;
import com.jecstar.etm.server.core.elasticsearch.domain.converter.SearchStatsConverter;
import com.jecstar.etm.server.core.elasticsearch.domain.converter.StoreStatsConverter;

public class CommonStats {

    @JsonField(value = "docs", converterClass = DocsStatsConverter.class)
    private DocsStats docs;
    @JsonField(value = "store", converterClass = StoreStatsConverter.class)
    private StoreStats store;
    @JsonField(value = "indexing", converterClass = IndexingStatsConverter.class)
    private IndexingStats indexing;
    @JsonField(value = "search", converterClass = SearchStatsConverter.class)
    private SearchStats search;

    public DocsStats getDocs() {
        return this.docs;
    }

    public StoreStats getStore() {
        return this.store;
    }

    public IndexingStats getIndexing() {
        return this.indexing;
    }

    public SearchStats getSearch() {
        return this.search;
    }
}
