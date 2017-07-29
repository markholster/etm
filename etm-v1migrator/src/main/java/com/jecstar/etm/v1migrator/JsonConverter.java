package com.jecstar.etm.v1migrator;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

class JsonConverter {

	@SuppressWarnings("unchecked")
	public Map<String, Object> getObject(String tag, Map<String, Object> valueMap) {
		if (valueMap.containsKey(tag)) {
			return (Map<String, Object>) valueMap.get(tag);
		}
		return Collections.emptyMap();
	}

	@SuppressWarnings("unchecked")
	public <T extends Collection<?>>T getArray(String tag, Map<String, Object> valueMap) {
		if (valueMap.containsKey(tag)) {
			return (T) valueMap.get(tag);
		}
		return null;
	}

	public Boolean getBoolean(String tag, Map<String, Object> valueMap) {
		return getBoolean(tag, valueMap, null);
	}

	private Boolean getBoolean(String tag, Map<String, Object> valueMap, Boolean defaultValue) {
		if (valueMap.get(tag) != null) {
			return (Boolean) valueMap.get(tag);
		}
		return defaultValue;
	}

	public String getString(String tag, Map<String, Object> valueMap) {
		return getString(tag, valueMap, null);
	}

	private String getString(String tag, Map<String, Object> valueMap, String defaultValue) {
		if (valueMap.get(tag) != null) {
			return valueMap.get(tag).toString();
		}
		return defaultValue;
	}

	public Integer getInteger(String tag, Map<String, Object> valueMap) {
		return getInteger(tag, valueMap, null);
	}

	private Integer getInteger(String tag, Map<String, Object> valueMap, Integer defaultValue) {
		if (valueMap.get(tag) != null) {
			return ((Number) valueMap.get(tag)).intValue();
		}
		return defaultValue;
	}

	public Long getLong(String tag, Map<String, Object> valueMap) {
		return getLong(tag, valueMap, null);
	}

	private Long getLong(String tag, Map<String, Object> valueMap, Long defaultValue) {
		if (valueMap.get(tag) != null) {
			return ((Number) valueMap.get(tag)).longValue();
		}
		return defaultValue;
	}

	public Double getDouble(String tag, Map<String, Object> valueMap) {
		return getDouble(tag, valueMap, null);
	}

	private Double getDouble(String tag, Map<String, Object> valueMap, Double defaultValue) {
		if (valueMap.get(tag) != null) {
			return ((Number) valueMap.get(tag)).doubleValue();
		}
		return defaultValue;
	}

	public ZonedDateTime getZonedDateTime(String tag, Map<String, Object> valueMap) {
		String stringDate = getString(tag, valueMap);
		if (stringDate == null) {
			return null;
		}
		try {
			return ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(stringDate)), ZoneOffset.UTC);
		} catch (NumberFormatException nfe) {
			System.out.println(
					"'" + stringDate + "' could not be converted to a long. ZonedDateTime could not be determined.");
		}
		return null;
	}

	public Object unescapeObjectFromJsonNameValuePair(String tag, Map<String, Object> valueMap) {
		if (valueMap.containsKey(tag + "_as_number")) {
			return valueMap.get(tag + "_as_number");
		}
		if (valueMap.containsKey(tag + "_as_boolean")) {
			return valueMap.get(tag + "_as_boolean");
		}
		if (valueMap.containsKey(tag + "_as_date")) {
			return new Date((Long) valueMap.get(tag + "_as_date"));
		}
		if (valueMap.containsKey(tag)) {
			return valueMap.get(tag);
		}
		return null;
	}
}
