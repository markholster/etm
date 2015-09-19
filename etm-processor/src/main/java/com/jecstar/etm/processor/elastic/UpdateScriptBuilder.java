package com.jecstar.etm.processor.elastic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jecstar.etm.core.domain.EndpointHandler;
import com.jecstar.etm.core.domain.TelemetryEvent;
import com.jecstar.etm.core.domain.converter.TelemetryEventConverterTags;

/**
 * Class capable of building the update map that can be provided to the scripts.. This class reuses some
 * objects in favor of performance, and hence is not thread safe!.
 * 
 * @author mark
 */
public class UpdateScriptBuilder {

	private Map<String, Object> parameters = new HashMap<String, Object>();
	
	UpdateScriptBuilder() {
	}

	Map<String, Object> createUpdateParameterMap(TelemetryEvent event, TelemetryEventConverterTags tags) {
		this.parameters.clear();
		addValueSetterToScript(tags.getCorrelationIdTag(), event.correlationId);
		addMapAppenderToScript(tags.getCorrelationDataTag(), event.correlationData);
		addValueSetterToScript(tags.getEndpointTag(), event.endpoint);
		addValueSetterToScript(tags.getExpiryTag(), event.expiry == null ? null : event.expiry.toInstant().toEpochMilli());
		addMapAppenderToScript(tags.getExtractedDataTag(), event.extractedData);
		addMapAppenderToScript(tags.getMetadataTag(), event.metadata);
		addValueSetterToScript(tags.getNameTag(), event.name);
		addValueSetterToScript(tags.getPackagingTag(), event.packaging);
		addValueSetterToScript(tags.getPayloadTag(), event.payload);
		addValueSetterToScript(tags.getPayloadFormatTag(), event.payloadFormat == null ? null : event.payloadFormat.name());
		if (event.readingEndpointHandlers.size() > 0) {
			List<Map<String, Object>> readingEndpointHandlers = new ArrayList<Map<String, Object>>();
			for (EndpointHandler endpointHandler : event.readingEndpointHandlers) {
				if (endpointHandler.isSet()) {
					readingEndpointHandlers.add(createEndpointHandlerMap(endpointHandler, tags));
				}
			}
			addArrayAppenderToScript(tags.getReadingEndpointHandlersTag(), readingEndpointHandlers);
		} else {
			addArrayAppenderToScript(tags.getReadingEndpointHandlersTag(), null);
		}
		addValueSetterToScript(tags.getTransportTag(), event.transport == null ? null : event.transport.name());
		if (event.writingEndpointHandler.isSet()) {
			addValueSetterToScript(tags.getWritingEndpointHandlerTag(), createEndpointHandlerMap(event.writingEndpointHandler, tags));
		} else {
			addValueSetterToScript(tags.getWritingEndpointHandlerTag(), null);
		}
		return this.parameters;
	}
	
	Map<String, Object> createRequestUpdateScriptFromResponse(EndpointHandler endpointHandler, TelemetryEventConverterTags tags) {
		this.parameters.clear();
		this.parameters.put(tags.getResponseHandlingTimeTag(), endpointHandler.handlingTime.toInstant().toEpochMilli());
		return this.parameters;
	}
	
	private void addValueSetterToScript(String tag, Object value) {
		this.parameters.put(tag, value);
	}
	
	private void addArrayAppenderToScript(String tag, Object value) {
		this.parameters.put(tag, value);		
	}
	
	private void addMapAppenderToScript(String tag, Map<String, String> map) {
		if (map == null || map.size() <= 0) {
			addArrayAppenderToScript(tag, null);
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
}
