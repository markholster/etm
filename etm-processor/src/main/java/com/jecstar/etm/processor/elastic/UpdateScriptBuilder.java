package com.jecstar.etm.processor.elastic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jecstar.etm.core.domain.EndpointHandler;
import com.jecstar.etm.core.domain.TelemetryEvent;
import com.jecstar.etm.core.domain.converter.TelemetryEventConverterTags;

public class UpdateScriptBuilder {

	private final UpdateScriptResponse response = new UpdateScriptResponse();
	
	UpdateScriptBuilder() {
	}

	UpdateScriptResponse createUpdateScript(TelemetryEvent event, TelemetryEventConverterTags tags) {
		this.response.initialize();
		if (!event.writingEndpointHandler.isSet() && event.readingEndpointHandlers.size() > 0) {
			this.response.script.append("if (ctx._source." + tags.getReadingEndpointHandlersTag() + ") {ctx._source." + tags.getReadingEndpointHandlersTag() + " += " + tags.getReadingEndpointHandlersTag()+ "} else {ctx._source." + tags.getReadingEndpointHandlersTag() + " = " + tags.getReadingEndpointHandlersTag() + "};");
			List<Map<String, Object>> readingEndpointHandlers = new ArrayList<Map<String, Object>>();
			for (EndpointHandler endpointHandler : event.readingEndpointHandlers) {
				Map<String, Object> endpointHandlerMap = new HashMap<String, Object>();
				if (endpointHandler.handlingTime != null) {
					endpointHandlerMap.put(tags.getEndpointHandlerHandlingTimeTag(), endpointHandler.handlingTime.toInstant().toEpochMilli());
				}
				if (endpointHandler.application.isSet()) {
					Map<String, Object> applicationMap = new HashMap<String, Object>();
					if (endpointHandler.application.name != null) {
						applicationMap.put(tags.getApplicationNameTag(), endpointHandler.application.name);
					}
					if (endpointHandler.application.version != null) {
						applicationMap.put(tags.getApplicationVersionTag(), endpointHandler.application.version);
					}
					if (endpointHandler.application.instance != null) {
						applicationMap.put(tags.getApplicationInstanceTag(), endpointHandler.application.instance);
					}
					if (endpointHandler.application.principal != null) {
						applicationMap.put(tags.getApplicationPrincipalTag(), endpointHandler.application.principal);
					}
					endpointHandlerMap.put(tags.getEndpointHandlerApplicationTag(), applicationMap);
				}
				readingEndpointHandlers.add(endpointHandlerMap);
			}
			this.response.parameters.put(tags.getReadingEndpointHandlersTag(), readingEndpointHandlers);
		}
		return this.response;
	}
	
	public class UpdateScriptResponse {
		
		private StringBuilder script = new StringBuilder();
		private Map<String, Object> parameters = new HashMap<String, Object>();
		
		private void initialize() {
			this.script.setLength(0);
			this.parameters.clear();
		}

		public String getScript() {
			return this.script.toString();
		}
		
		public Map<String, Object> getParameters() {
			return this.parameters;
		}
		
		
	}
}
