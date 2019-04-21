package com.jecstar.etm.server.core.persisting;

import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.ClearScrollRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchScrollRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public class ScrollableSearch implements Iterable<SearchHit>, Iterator<SearchHit> {

    private final DataRepository dataRepository;
    private final SearchRequestBuilder searchRequestBuilder;
    private final int startIx;
    private final int scrollSize;

    private SearchResponse response;
    private String currentScrollId;
    private final Set<String> scrollIds = new HashSet<>();
    private boolean nextBatchRequired = false;
    private int currentIndexInResponse = 0;

    public ScrollableSearch(DataRepository dataRepository, SearchRequestBuilder builder) {
        this(dataRepository, builder, 0);
    }

    public ScrollableSearch(DataRepository dataRepository, SearchRequestBuilder builder, int startIx) {
        this(dataRepository, builder, startIx, 25);
    }

    private ScrollableSearch(DataRepository dataRepository, SearchRequestBuilder builder, int startIx, int scrollSize) {
        this.dataRepository = dataRepository;
        this.searchRequestBuilder = builder;
        this.startIx = startIx;
        this.scrollSize = scrollSize;
    }

    @Override
    public boolean hasNext() {
        if (this.response == null) {
            executeSearch();
        }
        if (this.response.getHits().getHits().length == this.currentIndexInResponse && this.nextBatchRequired) {
            scrollToNext();
        }
        boolean hasNext = this.currentIndexInResponse < this.response.getHits().getHits().length;
        if (!hasNext) {
            clearScrollIds();
        }
        return hasNext;
    }

    @Override
    public Iterator<SearchHit> iterator() {
        return this;
    }

    @Override
    public SearchHit next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return this.response.getHits().getHits()[this.currentIndexInResponse++];
    }

    public void clearScrollIds() {
        ClearScrollRequestBuilder builder = new ClearScrollRequestBuilder();
        for (String scrollId : this.scrollIds) {
            builder.addScrollId(scrollId);
        }
        this.dataRepository.clearScroll(builder);
    }

    public long getNumberOfHits() {
        if (this.response == null) {
            executeSearch();
        }
        return this.response.getHits().getTotalHits().value;
    }

    private void executeSearch() {
        this.searchRequestBuilder
                .setScroll(TimeValue.timeValueSeconds(30))
                .setSize(this.scrollSize)
                .setFrom(this.startIx);
        this.response = this.dataRepository.search(this.searchRequestBuilder);

        this.currentIndexInResponse = 0;
        this.currentScrollId = this.response.getScrollId();
        this.scrollIds.add(this.currentScrollId);
        this.nextBatchRequired = this.scrollSize == this.response.getHits().getHits().length;
    }

    private void scrollToNext() {
        SearchScrollRequestBuilder builder = new SearchScrollRequestBuilder(this.currentScrollId).setScroll(TimeValue.timeValueSeconds(30));
        this.response = this.dataRepository.scroll(builder);

        this.currentIndexInResponse = 0;
        this.currentScrollId = this.response.getScrollId();
        this.scrollIds.add(this.currentScrollId);
        this.nextBatchRequired = this.scrollSize == this.response.getHits().getHits().length;
//
    }
}
