package com.jecstar.etm.server.core.enhancers;

import java.time.ZonedDateTime;

import com.jecstar.etm.domain.TelemetryEvent;
import com.jecstar.etm.server.core.parsers.ExpressionParser;

public abstract class AbstractConfigurableTelemetryEventEnhancer extends AbstractDefaultTelemetryEventEnhancer {
	
	private ExpressionParser eventNameExpressionParser;
	
	public void setEventNameExpressionParser(ExpressionParser eventNameExpressionParser) {
		this.eventNameExpressionParser = eventNameExpressionParser;
	}
	
	@Override
	protected void doEnhance(TelemetryEvent<?> event, ZonedDateTime enhanceTime) {
		super.doEnhance(event, enhanceTime);
		String value = executeExpressionParser(this.eventNameExpressionParser, event.payload);
		if (value != null) {
			event.name = value;
		}
	}

	private String executeExpressionParser(ExpressionParser expressionParser, String payload) {
		if (expressionParser == null) {
			return null;
		}
		return expressionParser.evaluate(payload);
	}
}
