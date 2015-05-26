package com.jecstar.etm.processor.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jecstar.etm.core.parsers.ExpressionParser;

public class EndpointConfigResult {

	public List<ExpressionParser> readingApplicationParsers = new ArrayList<ExpressionParser>();
	public List<ExpressionParser> writingApplicationParsers = new ArrayList<ExpressionParser>();
	public List<ExpressionParser> eventNameParsers = new ArrayList<ExpressionParser>();
	public List<ExpressionParser> transactionNameParsers = new ArrayList<ExpressionParser>();
	public Map<String, ExpressionParser> correlationDataParsers = new HashMap<String, ExpressionParser>();
	
	// process state
	public long retrieved;

	public EndpointConfigResult initialize() {
	    if (this.readingApplicationParsers != null) {
	    	this.readingApplicationParsers.clear();
	    }
	    if (this.writingApplicationParsers != null) {
	    	this.writingApplicationParsers.clear();
	    }
	    if (this.eventNameParsers != null) {
	    	this.eventNameParsers.clear();
	    }
	    if (this.transactionNameParsers != null) {
	    	this.transactionNameParsers.clear();
	    }
	    if (this.correlationDataParsers != null) {
	    	this.correlationDataParsers.clear();
	    }
	    return this;
    }
	
	
}
