package com.holster.etm.processor.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.holster.etm.processor.TelemetryEventDirection;
import com.holster.etm.processor.parsers.ExpressionParser;

public class EndpointConfigResult {

	public List<ExpressionParser> applicationParsers;
	public List<ExpressionParser> eventNameParsers;
	public List<ExpressionParser> transactionNameParsers;
	public Map<String, ExpressionParser> correlationDataParsers = new HashMap<String, ExpressionParser>();
	public TelemetryEventDirection eventDirection;
	
	// process state
	public long retrieved;
	
	
}
