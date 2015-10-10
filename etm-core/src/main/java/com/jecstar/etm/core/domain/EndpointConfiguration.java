package com.jecstar.etm.core.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jecstar.etm.core.parsers.ExpressionParser;

public class EndpointConfiguration {

	public List<ExpressionParser> readingApplicationParsers = new ArrayList<ExpressionParser>();
	public List<ExpressionParser> writingApplicationParsers = new ArrayList<ExpressionParser>();
	public List<ExpressionParser> eventNameParsers = new ArrayList<ExpressionParser>();
	public Map<String, ExpressionParser> correlationDataParsers = new HashMap<String, ExpressionParser>();
	public Map<String, ExpressionParser> extractionDataParsers = new HashMap<String, ExpressionParser>();
	
	// process state
	public long retrievalTimestamp;

	public EndpointConfiguration initialize() {
	    if (this.readingApplicationParsers != null) {
	    	this.readingApplicationParsers.clear();
	    }
	    if (this.writingApplicationParsers != null) {
	    	this.writingApplicationParsers.clear();
	    }
	    if (this.eventNameParsers != null) {
	    	this.eventNameParsers.clear();
	    }
	    if (this.correlationDataParsers != null) {
	    	this.correlationDataParsers.clear();
	    }
	    if (this.extractionDataParsers != null) {
	    	this.extractionDataParsers.clear();
	    }
	    return this;
    }
}
