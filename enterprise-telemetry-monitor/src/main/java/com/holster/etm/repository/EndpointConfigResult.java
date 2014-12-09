package com.holster.etm.repository;

import java.util.List;

import com.holster.etm.ExpressionParser;

public class EndpointConfigResult {

	public List<ExpressionParser> applicationParsers;
	public List<ExpressionParser> eventNameParsers;

	// process state
	public long retrieved;
}
