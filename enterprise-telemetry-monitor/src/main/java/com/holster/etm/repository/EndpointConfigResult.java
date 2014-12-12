package com.holster.etm.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.holster.etm.parsers.ExpressionParser;

public class EndpointConfigResult {

	public List<ExpressionParser> applicationParsers;
	public List<ExpressionParser> eventNameParsers;
	public Map<String, ExpressionParser> correlationDataParsers = new HashMap<String, ExpressionParser>();
	
	// process state
	public long retrieved;
	
}
