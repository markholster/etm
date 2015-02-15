package com.holster.etm.gui.rest.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.holster.etm.core.TelemetryEventDirection;
import com.holster.etm.core.parsers.ExpressionParser;
import com.holster.etm.core.sla.SlaRule;

public class EndpointConfiguration {

	public String name;
	
	public TelemetryEventDirection direction;
	
	public List<ExpressionParser> applicationParsers = new ArrayList<ExpressionParser>();
	
	public List<ExpressionParser> eventNameParsers = new ArrayList<ExpressionParser>();
	
	public List<ExpressionParser> transactionNameParsers = new ArrayList<ExpressionParser>();
	
	public Map<String, ExpressionParser> correlationParsers = new LinkedHashMap<String, ExpressionParser>();
	
	public Map<String, SlaRule> slaRules = new LinkedHashMap<String, SlaRule>();
	
}
