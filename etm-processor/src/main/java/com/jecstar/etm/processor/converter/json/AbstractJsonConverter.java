package com.jecstar.etm.processor.converter.json;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jecstar.etm.core.EtmException;

public abstract class AbstractJsonConverter {
	
	private final ObjectMapper objectMapper = new ObjectMapper();
	
	@SuppressWarnings("unchecked")
	protected Map<String, Object> toMap(String json) {
		try {
			return this.objectMapper.readValue(json, HashMap.class);
		} catch (IOException e) {
			throw new EtmException(EtmException.INVALID_JSON_EXPRESSION, e);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected Map<String, Object> getObject(String tag, Map<String, Object> valueMap) {
		if (valueMap.containsKey(tag)) {
			return (Map<String, Object>) valueMap.get(tag);
		}
		return Collections.emptyMap();
	}
	
	@SuppressWarnings("unchecked")
	protected List<Map<String, Object>> getArray(String tag, Map<String, Object> valueMap) {
		if (valueMap.containsKey(tag)) {
			return (List<Map<String, Object>>) valueMap.get(tag);
		}
		return Collections.emptyList();
	}
	
	protected String getString(String tag, Map<String, Object> valueMap) {
		if (valueMap.containsKey(tag)) {
			return valueMap.get(tag).toString();
		}
		return null;
	}
	
	protected Integer getInteger(String tag, Map<String, Object> valueMap) {
		if (valueMap.containsKey(tag)) {
			return ((Number)valueMap.get(tag)).intValue();
		}
		return null;
	}
	
	protected Double getDouble(String tag, Map<String, Object> valueMap) {
		if (valueMap.containsKey(tag)) {
			return ((Double)valueMap.get(tag)).doubleValue();
		}
		return null;
	}
	
	protected ZonedDateTime getZonedDateTime(String tag, Map<String, Object> valueMap) {
		String stringDate = getString(tag, valueMap);
		if (stringDate == null) {
			return null;
		}
		try {
			return ZonedDateTime.parse(stringDate);
		} catch (DateTimeParseException dtpe) {
			try {
				return ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(stringDate)), ZoneOffset.UTC);
			} catch (NumberFormatException nfe) {
				return null;
			}
		}
	}

	protected boolean addStringElementToJsonBuffer(String elementName, String elementValue, StringBuilder buffer, boolean firstElement) {
		if (elementValue == null) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append("\"" + escapeToJson(elementName) + "\": \"" + escapeToJson(elementValue) + "\"");
		return true;
	}

	protected boolean addLongElementToJsonBuffer(String elementName, Long elementValue, StringBuilder buffer, boolean firstElement) {
		if (elementValue == null) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append("\"" + escapeToJson(elementName) + "\": " + elementValue);
		return true;
	}
	
	protected boolean addDoubleElementToJsonBuffer(String elementName, Double elementValue, StringBuilder buffer, boolean firstElement) {
		if (elementValue == null) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append("\"" + escapeToJson(elementName) + "\": " + elementValue);
		return true;
	}
	
	protected boolean addIntegerElementToJsonBuffer(String elementName, Integer elementValue, StringBuilder buffer, boolean firstElement) {
		if (elementValue == null) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append("\"" + escapeToJson(elementName) + "\": " + elementValue);
		return true;
	}
	
	protected boolean addInetAddressElementToJsonBuffer(String elementName, InetAddress elementValue, StringBuilder buffer, boolean firstElement) {
		if (elementValue == null) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append("\"" + escapeToJson(elementName) + "\": \"" + elementValue.getHostAddress() + "\"");
		return true;
	}

	
	protected String escapeToJson(String value) {
		return value.replace("\"", "\\\"");
	}


}
