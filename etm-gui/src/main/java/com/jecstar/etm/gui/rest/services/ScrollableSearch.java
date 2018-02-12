package com.jecstar.etm.gui.rest.services;

import org.elasticsearch.action.search.ClearScrollRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public class ScrollableSearch implements Iterable<SearchHit>, Iterator<SearchHit> {

    private final Client client;
    private final SearchRequestBuilder searchRequestBuilder;
    private final long requestTimeout;
    private final int startIx;
    private final int scrollSize;

    private SearchResponse response;
    private String currentScrollId;
    private final Set<String> scrollIds = new HashSet<>();
    private boolean nextBatchRequired = false;
    private int currentIndexInResponse = 0;

    public ScrollableSearch(Client client, SearchRequestBuilder searchRequestBuilder) {
        this(client, searchRequestBuilder, 0);
    }

    public ScrollableSearch(Client client, SearchRequestBuilder searchRequestBuilder, int startIx) {
        this(client, searchRequestBuilder, startIx, 25);
    }

    private ScrollableSearch(Client client, SearchRequestBuilder searchRequestBuilder, int startIx, int scrollSize) {
        this.client = client;
        this.searchRequestBuilder = searchRequestBuilder;
        this.requestTimeout = searchRequestBuilder.request().source().timeout().getMillis();
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
        ClearScrollRequestBuilder clearScrollRequestBuilder = this.client.prepareClearScroll();
        for (String scrollId : this.scrollIds) {
            clearScrollRequestBuilder.addScrollId(scrollId);
        }
        clearScrollRequestBuilder.execute();
    }

    public long getNumberOfHits() {
        if (this.response == null) {
            executeSearch();
        }
        return this.response.getHits().getTotalHits();
    }

    private void executeSearch() {
        this.response = this.searchRequestBuilder
                .setSize(this.scrollSize)
                .setFrom(this.startIx)
                .setScroll(new Scroll(TimeValue.timeValueSeconds(30)))
                .get();
        this.currentIndexInResponse = 0;
        this.currentScrollId = this.response.getScrollId();
        this.scrollIds.add(this.currentScrollId);
        this.nextBatchRequired = this.scrollSize == this.response.getHits().getHits().length;
    }

    private void scrollToNext() {
        this.response = client.prepareSearchScroll(this.currentScrollId)
                .setScroll(TimeValue.timeValueSeconds(30))
                .get(TimeValue.timeValueMillis(this.requestTimeout));
        this.currentIndexInResponse = 0;
        this.currentScrollId = this.response.getScrollId();
        this.scrollIds.add(this.currentScrollId);
        this.nextBatchRequired = this.scrollSize == this.response.getHits().getHits().length;

    }
}
