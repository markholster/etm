package com.jecstar.etm.processor.elastic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jecstar.etm.core.domain.EndpointHandler;
import com.jecstar.etm.core.domain.TelemetryEvent;
import com.jecstar.etm.core.domain.converter.TelemetryEventConverterTags;

/**
 * Class capable of building an elastic update script. This class reuses some
 * objects in favor of performance, and hence is not thread safe!.
 * 
 * @author mark
 */
public class UpdateScriptBuilder {

	private final UpdateScriptResponse response = new UpdateScriptResponse();
	
	UpdateScriptBuilder() {
	}

	UpdateScriptResponse createUpdateScript(TelemetryEvent event, TelemetryEventConverterTags tags) {
		this.response.initialize();
		addValueSetterToScript(tags.getCorrelationIdTag(), event.correlationId);
		addMapAppenderToScript(tags.getCorrelationDataTag(), event.correlationData);
		addValueSetterToScript(tags.getCorrelationIdTag(), event.endpoint);
		if (event.expiry != null) {
			addValueSetterToScript(tags.getExpiryTag(), event.expiry.toInstant().toEpochMilli());
		}
		addMapAppenderToScript(tags.getExtractedDataTag(), event.extractedData);
		addMapAppenderToScript(tags.getMetadataTag(), event.metadata);
		addValueSetterToScript(tags.getNameTag(), event.name);
		addValueSetterToScript(tags.getPackagingTag(), event.packaging);
		addValueSetterToScript(tags.getPayloadTag(), event.payload);
		if (event.payloadFormat != null) {
			addValueSetterToScript(tags.getPayloadFormatTag(), event.payloadFormat.name());
		}
		if (event.readingEndpointHandlers.size() > 0) {
			List<Map<String, Object>> readingEndpointHandlers = new ArrayList<Map<String, Object>>();
			for (EndpointHandler endpointHandler : event.readingEndpointHandlers) {
				if (endpointHandler.isSet()) {
					readingEndpointHandlers.add(createEndpointHandlerMap(endpointHandler, tags));
				}
			}
			addArrayAppenderToScript(tags.getReadingEndpointHandlersTag(), readingEndpointHandlers);
		}
		if (event.transport != null) {
			addValueSetterToScript(tags.getPackagingTag(), event.transport.name());
		}
		if (event.writingEndpointHandler.isSet()) {
			addValueSetterToScript(tags.getWritingEndpointHandlerTag(), createEndpointHandlerMap(event.writingEndpointHandler, tags));
		}
		if (event.isRequest() && event.expiry != null && event.writingEndpointHandler.handlingTime != null) {
			long responseTime = event.expiry.toInstant().toEpochMilli() - event.writingEndpointHandler.handlingTime.toInstant().toEpochMilli();
			this.response.script.append("if (!ctx._source." + tags.getResponseTimeTag() + ") {ctx._source." + tags.getResponseTimeTag() + " = " + responseTime  + "};" );
		}
		return this.response;
	}
	
	private void addValueSetterToScript(String tag, Object value) {
		if (value == null) {
			return;
		}
		this.response.script.append("ctx._source." + tag + " = " + tag +";");
		this.response.parameters.put(tag, value);
	}
	
	private void addArrayAppenderToScript(String tag, Object value) {
		if (value == null) {
			return;
		}
		this.response.script.append("if (ctx._source." + tag + ") {ctx._source." + tag + " += " + tag+ "} else {ctx._source." + tag + " = " + tag + "};");
		this.response.parameters.put(tag, value);		
	}
	
	private void addMapAppenderToScript(String tag, Map<String, String> map) {
		if (map == null || map.size() <= 0) {
			return;
		}
		List<Map<String, Object>> jsonData = new ArrayList<Map<String, Object>>();
		for (String key : map.keySet()) {
			Map<String, Object> data = new HashMap<String, Object>();
			data.put(key, map.get(key));
			jsonData.add(data);
		}
		addArrayAppenderToScript(tag, jsonData);
	}

	
	private Map<String, Object> createEndpointHandlerMap(EndpointHandler endpointHandler, TelemetryEventConverterTags tags) {
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
		return endpointHandlerMap;	
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
