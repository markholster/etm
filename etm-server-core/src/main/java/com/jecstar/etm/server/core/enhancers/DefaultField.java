package com.jecstar.etm.server.core.enhancers;

import java.util.ArrayList;
import java.util.List;

import com.jecstar.etm.server.core.parsers.ExpressionParser;

public class DefaultField {
	
	public enum WritePolicy {
		
		OVERWRITE_WHEN_FOUND, ALWAYS_OVERWRITE, WHEN_EMPTY; 
		
		public static WritePolicy safeValueOf(String value) {
		if (value == null) {
			return null;
		}
		try {
			return WritePolicy.valueOf(value.toUpperCase());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
	
	};


	private String name;
	
	private WritePolicy writePolicy = WritePolicy.WHEN_EMPTY;
	
	private List<ExpressionParser> parsers = new ArrayList<>();
	
	public DefaultField(String name) {
		this.name = name;
	}

	public WritePolicy getWritePolicy() {
		return this.writePolicy;
	}

	public void setWritePolicy(WritePolicy writePolicy) {
		if (writePolicy != null) {
			this.writePolicy = writePolicy;
		}
	}

	public String getName() {
		return name;
	}

	public List<ExpressionParser> getParsers() {
		return this.parsers;
	}
	
	public void addParser(ExpressionParser expressionParser) {
		this.parsers.add(expressionParser);
	}
	
	public void addParsers(List<ExpressionParser> expressionParsers) {
		this.parsers.addAll(expressionParsers);
	}
	
}
