package com.jecstar.etm.core.domain.converter.json;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
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
	protected <T extends Collection<?>> T getArray(String tag, Map<String, Object> valueMap) {
		if (valueMap.containsKey(tag)) {
			return (T) valueMap.get(tag);
		}
		return null;
	}
	
	protected String getString(String tag, Map<String, Object> valueMap) {
		if (valueMap.containsKey(tag)) {
			return valueMap.get(tag).toString();
		}
		return null;
	}
	
	protected Integer getInteger(String tag, Map<String, Object> valueMap) {
		return getInteger(tag, valueMap, null);
	}
	
	protected Integer getInteger(String tag, Map<String, Object> valueMap, Integer defaultValue) {
		if (valueMap.containsKey(tag)) {
			return ((Number)valueMap.get(tag)).intValue();
		}
		return defaultValue;
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
	
	protected boolean addInetAddressElementToJsonBuffer(String hostAddressTag, String hostNameTag, InetAddress elementValue, StringBuilder buffer, boolean firstElement) {
		if (elementValue == null) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append("\"" + escapeToJson(hostAddressTag) + "\": \"" + escapeToJson(elementValue.getHostAddress()) + "\"");
		// TODO documenteer dat de getHostName heel duurt is als er geen gebruikt wordt gemaakt van een meegegeven hostname.
		buffer.append(", \"" + escapeToJson(hostNameTag) + "\": \"" + escapeToJson(elementValue.getHostName()) + "\"");
		return true;
	}
	
	protected boolean addSetElementToJsonBuffer(String elementName, Set<String> elementValues, StringBuilder buffer, boolean firstElement) {
		if (elementValues.size() < 1) {
			return false;
		}
		if (!firstElement) {
			buffer.append(", ");
		}
		buffer.append("\"" + elementName + "\": [");
		buffer.append(elementValues.stream()
				.map(c -> "\"" + escapeToJson(c) + "\"")
				.sorted()
				.collect(Collectors.joining(", ")));
		buffer.append("]");
		return true;
	}
	
	protected String escapeToJson(String value) {
	    try {
	    	String newValue = this.objectMapper.writeValueAsString(value);
			return newValue.substring(1, newValue.length() - 1);
		} catch (JsonProcessingException e) {
			// Try to make the best of it. Unicodes outside of the range 32-0x7f should actually be escaped as well.
			return value
					.replace("\"", "\\\"")
					.replace("\\", "\\\\")
					.replace("/", "\\/")
					.replace("\b", "\\b")
	    			.replace("\n", "\\n")
					.replace("\t", "\\t")
					.replace("\f", "\\f")
					.replace("\r", "\\r");		
		}
	}

	/**
	 * Escape an object to a json name/value pair. The value is always
	 * considered to be a String, but if the value happens to be a
	 * <code>Number</code>, <code>Boolean</code> or <code>Date</code> a second
	 * name value/pair is added to the response with the specific value.
	 * 
	 * @param name
	 *            The name part of the name/value pair.
	 * @param value
	 *            The value part of the name/value pair.
	 * @return The name/value escaped to json.
	 */
	protected String escapeObjectToJsonNameValuePair(String name, Object value) {
		if (value == null) {
			return "\"" + escapeToJson(name) + "\": " + "null";
		} else if (value instanceof Number) {
			return "\"" + escapeToJson(name) + "\": \"" + value.toString() + "\", \"" + escapeToJson(name) + "_as_number\": " + value.toString();
		} else if (value instanceof Boolean) {
			return "\"" + escapeToJson(name) + "\": \"" + value.toString() + "\", \"" + escapeToJson(name) + "_as_boolean\": " + value.toString();
		} else if (value instanceof Date) {
			return "\"" + escapeToJson(name) + "\": \"" + value.toString() + "\", \"" + escapeToJson(name) + "_as_date\": " + ((Date)value).getTime();
		}
		return "\"" + escapeToJson(name) + "\": \"" + escapeToJson(value.toString()) + "\"";		
	}
	
	
	protected Object unescapeObjectFromJsonNameValuePair(String tag, Map<String, Object> valueMap) {
		if (valueMap.containsKey(tag + "_as_number")) {
			return valueMap.get(tag + "_as_number");
		} else if (valueMap.containsKey(tag + "_as_boolean")) {
			return valueMap.get(tag + "_as_boolean");
		} else if (valueMap.containsKey(tag + "_as_date")) {
			return new Date((long)valueMap.get(tag + "_as_date"));
		} else if (valueMap.containsKey(tag)) {
			return valueMap.get(tag);
		}
		return null;
	}

}
