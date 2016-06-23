package com.jecstar.etm.server.core.domain.converter.json;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jecstar.etm.domain.writers.json.JsonWriter;
import com.jecstar.etm.server.core.EtmException;

public class JsonConverter extends JsonWriter {
	
	private final ObjectMapper objectMapper = new ObjectMapper();
	
	@SuppressWarnings("unchecked")
	public Map<String, Object> toMap(String json) {
		try {
			return this.objectMapper.readValue(json, HashMap.class);
		} catch (IOException e) {
			throw new EtmException(EtmException.INVALID_JSON_EXPRESSION, e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, Object> getObject(String tag, Map<String, Object> valueMap) {
		if (valueMap.containsKey(tag)) {
			return (Map<String, Object>) valueMap.get(tag);
		}
		return Collections.emptyMap();
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Collection<?>> T getArray(String tag, Map<String, Object> valueMap) {
		if (valueMap.containsKey(tag)) {
			return (T) valueMap.get(tag);
		}
		return null;
	}
	
	public String getString(String tag, Map<String, Object> valueMap) {
		return getString(tag, valueMap, null);
	}
	
	public String getString(String tag, Map<String, Object> valueMap, String defaultValue) {
		if (valueMap.containsKey(tag)) {
			return valueMap.get(tag).toString();
		}
		return defaultValue;
	}
	
	public Integer getInteger(String tag, Map<String, Object> valueMap) {
		return getInteger(tag, valueMap, null);
	}
	
	public Integer getInteger(String tag, Map<String, Object> valueMap, Integer defaultValue) {
		if (valueMap.containsKey(tag)) {
			return ((Number)valueMap.get(tag)).intValue();
		}
		return defaultValue;
	}

	public Long getLong(String tag, Map<String, Object> valueMap) {
		return getLong(tag, valueMap, null);
	}

	public Long getLong(String tag, Map<String, Object> valueMap, Long defaultValue) {
		if (valueMap.containsKey(tag)) {
			return ((Number)valueMap.get(tag)).longValue();
		}
		return defaultValue;
	}
	
	public Double getDouble(String tag, Map<String, Object> valueMap) {
		if (valueMap.containsKey(tag)) {
			return ((Double)valueMap.get(tag)).doubleValue();
		}
		return null;
	}
	
	public ZonedDateTime getZonedDateTime(String tag, Map<String, Object> valueMap) {
		String stringDate = getString(tag, valueMap);
		if (stringDate == null) {
			return null;
		}
		try {
			return ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(stringDate)), ZoneOffset.UTC);
		} catch (NumberFormatException nfe) {
			return null;
		}
	}

	public Object unescapeObjectFromJsonNameValuePair(String tag, Map<String, Object> valueMap) {
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
