package com.jecstar.etm.processor.elastic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
		if (event.isRequest() && event.writingEndpointHandler.handlingTime != null) {
			if (event.writingEndpointHandler.application.name != null) {
				// If the response(s) are stored before the request, determine the response time immediately.
				this.response.script.append("if (ctx._source." + tags.getResponsesHandlingTimeTag() + ") {for (int i=0; i < ctx._source." + tags.getResponsesHandlingTimeTag() + ".size(); i++) {if (ctx._source." + tags.getResponsesHandlingTimeTag() + "[i]." + tags.getApplicationNameTag() + " == " + tags.getWritingEndpointHandlerTag() + "." + tags.getEndpointHandlerApplicationTag() + "." + tags.getApplicationNameTag() + ") {ctx._source." + tags.getResponseTimeTag() + " = ctx._source." + tags.getResponsesHandlingTimeTag() + "[i]." + tags.getEndpointHandlerHandlingTimeTag() + " - " + tags.getWritingEndpointHandlerTag() + "." + tags.getEndpointHandlerHandlingTimeTag() + "}} ; ctx._source.remove(\"" + tags.getResponsesHandlingTimeTag() + "\");}" );
			}
			if (event.expiry != null) {
				// The request has an expiry, and the responses aren't stored yet. Set the response time to the expiry time initially if no response time is present yet.
				if (event.writingEndpointHandler.application.name != null) {
					this.response.script.append(" else ");
				}
				long responseTime = event.expiry.toInstant().toEpochMilli() - event.writingEndpointHandler.handlingTime.toInstant().toEpochMilli();
				this.response.script.append("if (!ctx._source." + tags.getResponseTimeTag() + ") {ctx._source." + tags.getResponseTimeTag() + " = " + responseTime  + "}" );
			}
			this.response.script.append(";");
		}
		return this.response;
	}
	
	UpdateScriptResponse createRequestUpdateScriptFromResponse(TelemetryEvent event, TelemetryEventConverterTags tags) {
		this.response.initialize();
		this.response.script.append("if (ctx._source." + tags.getResponseTimeTag()+ ") {");
		// Response time present, so the writing application of the event is
		// stored with the expiry. Check if the given response has a reading
		// application which is the same as the writing application of the
		// request. If so, the responsetime can be calculated.
		this.response.script.append(event.readingEndpointHandlers.stream()
			.filter(p -> p.application.name != null && p.handlingTime != null)
			.map(f -> "if (ctx._source." + tags.getWritingEndpointHandlerTag() + "." + tags.getEndpointHandlerApplicationTag() + "." + tags.getApplicationNameTag() +" == '" + f.application.name + "' && ctx._source." + tags.getWritingEndpointHandlerTag() + "." + tags.getEndpointHandlerHandlingTimeTag() +") {ctx._source." + tags.getResponseTimeTag() +" = " + f.handlingTime.toInstant().toEpochMilli() + " - ctx._source." + tags.getWritingEndpointHandlerTag() + "." + tags.getEndpointHandlerHandlingTimeTag()+ "}")
			.collect(Collectors.joining(" else ")) + ";"
		);
		this.response.script.append("} else {");
		// No response time present, so we have to store/append the reading endpoint handlers in tags.getResponsesHandlingTimeTag(). 
		List<Map<String, Object>> values = event.readingEndpointHandlers.stream()
			.filter(p -> p.application.name != null && p.handlingTime != null)
			.map(f -> {
				Map<String, Object> map = new HashMap<String, Object>();
				map.put(tags.getApplicationNameTag(), f.application.name);
				map.put(tags.getEndpointHandlerHandlingTimeTag(), f.handlingTime.toInstant().toEpochMilli());
				return map;
				})
			.collect(Collectors.toList());
		addArrayAppenderToScript(tags.getResponsesHandlingTimeTag(), values);
		this.response.script.append("};");
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
		this.response.script.append("if (ctx._source." + tag + ") {ctx._source." + tag + " += " + tag + "} else {ctx._source." + tag + " = " + tag + "};");
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
			return new HashMap<String, Object>(this.parameters);
		}
		
		
	}
}
