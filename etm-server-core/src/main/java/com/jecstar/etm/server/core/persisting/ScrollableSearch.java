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

package com.jecstar.etm.server.core.persisting;

import com.jayway.jsonpath.JsonPath;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.ClearScrollRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchScrollRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public class ScrollableSearch implements Iterable<RedactedSearchHit>, Iterator<RedactedSearchHit> {

    private final DataRepository dataRepository;
    private final SearchRequestBuilder searchRequestBuilder;
    private final int startIx;
    private final int scrollSize;
    private final Set<JsonPath> redactedFields;

    private SearchResponse response;
    private String currentScrollId;
    private final Set<String> scrollIds = new HashSet<>();
    private boolean nextBatchRequired = false;
    private int currentIndexInResponse = 0;

    public ScrollableSearch(DataRepository dataRepository, SearchRequestBuilder builder, Set<JsonPath> redactedFields) {
        this(dataRepository, builder, redactedFields, 0);
    }

    public ScrollableSearch(DataRepository dataRepository, SearchRequestBuilder builder, Set<JsonPath> redactedFields, int startIx) {
        this(dataRepository, builder, redactedFields, startIx, 25);
    }

    private ScrollableSearch(DataRepository dataRepository, SearchRequestBuilder builder, Set<JsonPath> redactedFields, int startIx, int scrollSize) {
        this.dataRepository = dataRepository;
        this.searchRequestBuilder = builder;
        this.startIx = startIx;
        this.scrollSize = scrollSize;
        this.redactedFields = redactedFields;
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
    public Iterator<RedactedSearchHit> iterator() {
        return this;
    }

    @Override
    public RedactedSearchHit next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return new RedactedSearchHit(this.response.getHits().getHits()[this.currentIndexInResponse++], this.redactedFields);
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
    }
}
