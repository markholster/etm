package com.jecstar.etm.gui.rest.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.jecstar.etm.core.TelemetryEventDirection;
import com.jecstar.etm.core.parsers.ExpressionParser;

public class EndpointConfiguration {

	public String name;
	
	public TelemetryEventDirection direction;
	
	public List<ExpressionParser> applicationParsers = new ArrayList<ExpressionParser>();
	
	public List<ExpressionParser> eventNameParsers = new ArrayList<ExpressionParser>();
	
	public List<ExpressionParser> transactionNameParsers = new ArrayList<ExpressionParser>();
	
	public Map<String, ExpressionParser> correlationParsers = new LinkedHashMap<String, ExpressionParser>();
	
	
}
