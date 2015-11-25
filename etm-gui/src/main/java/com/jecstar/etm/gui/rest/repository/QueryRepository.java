package com.jecstar.etm.gui.rest.repository;

import java.io.IOException;

import org.elasticsearch.search.SearchHits;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

public interface QueryRepository {
	
	public void addEvents(SearchHits results, JsonGenerator generator) throws JsonGenerationException, IOException;

	public void addEvent(String eventId, String indexName, String indexType, JsonGenerator generator) throws JsonGenerationException, IOException;

	public void addEventOverview(String eventId, String indexName, String indexType, JsonGenerator generator) throws JsonGenerationException, IOException;
	
}
