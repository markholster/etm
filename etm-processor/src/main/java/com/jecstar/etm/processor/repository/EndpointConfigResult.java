package com.jecstar.etm.processor.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jecstar.etm.core.TelemetryEventDirection;
import com.jecstar.etm.core.parsers.ExpressionParser;

public class EndpointConfigResult {

	public List<ExpressionParser> applicationParsers = new ArrayList<ExpressionParser>();
	public List<ExpressionParser> eventNameParsers = new ArrayList<ExpressionParser>();
	public List<ExpressionParser> transactionNameParsers = new ArrayList<ExpressionParser>();
	public Map<String, ExpressionParser> correlationDataParsers = new HashMap<String, ExpressionParser>();
	public TelemetryEventDirection eventDirection;
	
	// process state
	public long retrieved;

	public EndpointConfigResult initialize() {
	    if (this.applicationParsers != null) {
	    	this.applicationParsers.clear();
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
	    this.eventDirection = null;
	    return this;
    }

	public void merge(EndpointConfigResult endpointConfigResult) {
		this.applicationParsers.addAll(endpointConfigResult.applicationParsers);
		this.eventNameParsers.addAll(endpointConfigResult.eventNameParsers);
		this.transactionNameParsers.addAll(endpointConfigResult.transactionNameParsers);
		this.correlationDataParsers.putAll(endpointConfigResult.correlationDataParsers);
		if (endpointConfigResult.eventDirection != null) {
			this.eventDirection = endpointConfigResult.eventDirection; 
		}
	}
	
	
}
