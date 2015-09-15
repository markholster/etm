package com.jecstar.etm.processor.elastic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jecstar.etm.core.domain.EndpointHandler;
import com.jecstar.etm.core.domain.TelemetryEvent;

public class UpdateScriptBuilder {

	private StringBuilder script = null;
	private Map<String, Object> parameters = null;
	private TelemetryEvent event;

	UpdateScriptBuilder(TelemetryEvent event) {
		this.event = event;
	}

	public String getScript() {
		if (this.script == null) {
			createUpdateScript();
		}
		return this.script.toString();
	}

	public Map<String, Object> getScriptParameters() {
		if (this.parameters == null) {
			createUpdateScript();
		}
		return this.parameters;
	}
	
	private void createUpdateScript() {
		this.script = new StringBuilder();
		this.parameters = new HashMap<String, Object>();
		if (!this.event.writingEndpointHandler.isSet() && this.event.readingEndpointHandlers.size() > 0) {
			this.script.append("if (ctx._source.reading_endpoint_handlers) {ctx._source.reading_endpoint_handlers += reading_endpoint_handlers} else {ctx._source.reading_endpoint_handlers = reading_endpoint_handlers};");
			List<Map<String, Object>> readingEndpointHandlers = new ArrayList<Map<String, Object>>();
			for (EndpointHandler endpointHandler : this.event.readingEndpointHandlers) {
				Map<String, Object> endpointHandlerMap = new HashMap<String, Object>();
				if (endpointHandler.handlingTime != null) {
					endpointHandlerMap.put("handling_time", endpointHandler.handlingTime.toInstant().toEpochMilli());
				}
				if (endpointHandler.application.isSet()) {
					Map<String, Object> applicationMap = new HashMap<String, Object>();
					if (endpointHandler.application.name != null) {
						applicationMap.put("name", endpointHandler.application.name);
					}
					endpointHandlerMap.put("application", applicationMap);
				}
				readingEndpointHandlers.add(endpointHandlerMap);
			}
			this.parameters.put("reading_endpoint_handlers", readingEndpointHandlers);
		}
	}
}
