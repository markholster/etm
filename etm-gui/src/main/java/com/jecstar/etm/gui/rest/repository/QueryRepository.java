package com.jecstar.etm.gui.rest.repository;

import java.io.IOException;
import java.util.UUID;

import org.apache.solr.common.SolrDocumentList;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

public interface QueryRepository {
	
	public void addEvents(SolrDocumentList results, JsonGenerator generator) throws JsonGenerationException, IOException;

	public void addEvent(UUID eventId, JsonGenerator generator) throws JsonGenerationException, IOException;

	public void addEventOverview(UUID eventId, JsonGenerator generator) throws JsonGenerationException, IOException;
	
}
