package com.jecstar.etm.server.core.domain.converter.json;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jecstar.etm.domain.writers.json.JsonWriter;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

public class JsonConverter extends JsonWriter {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(JsonConverter.class);
	
	private final ObjectMapper objectMapper = new ObjectMapper();
	
	@SuppressWarnings("unchecked")
	public Map<String, Object> toMap(String json) {
		try {
			return this.objectMapper.readValue(json, HashMap.class);
		} catch (IOException e) {
			throw new EtmException(EtmException.INVALID_JSON_EXPRESSION, e);
		}
	}
	
	public Map<String, Object> getObject(String tag, Map<String, Object> valueMap) {
		return getObject(tag, valueMap, Collections.emptyMap());
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, Object> getObject(String tag, Map<String, Object> valueMap, Map<String, Object> defaultValue) {
		if (valueMap.containsKey(tag)) {
			return (Map<String, Object>) valueMap.get(tag);
		}
		return defaultValue;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Collection<?>> T getArray(String tag, Map<String, Object> valueMap) {
		if (valueMap.containsKey(tag)) {
			return (T) valueMap.get(tag);
		}
		return null;
	}
	
	public Boolean getBoolean(String tag, Map<String, Object> valueMap) {
		return getBoolean(tag, valueMap, null);
	}
	
	public Boolean getBoolean(String tag, Map<String, Object> valueMap, Boolean defaultValue) {
		if (valueMap.get(tag) != null) {
			return (Boolean)valueMap.get(tag);
		}
		return defaultValue;
	}
	
	public String getString(String tag, Map<String, Object> valueMap) {
		return getString(tag, valueMap, null);
	}
	
	public String getString(String tag, Map<String, Object> valueMap, String defaultValue) {
		if (valueMap.get(tag) != null) {
			return valueMap.get(tag).toString();
		}
		return defaultValue;
	}
	
	public Integer getInteger(String tag, Map<String, Object> valueMap) {
		return getInteger(tag, valueMap, null);
	}
	
	public Integer getInteger(String tag, Map<String, Object> valueMap, Integer defaultValue) {
		if (valueMap.get(tag) != null) {
			return ((Number)valueMap.get(tag)).intValue();
		}
		return defaultValue;
	}

	public Long getLong(String tag, Map<String, Object> valueMap) {
		return getLong(tag, valueMap, null);
	}

	public Long getLong(String tag, Map<String, Object> valueMap, Long defaultValue) {
		if (valueMap.get(tag) != null) {
			return ((Number)valueMap.get(tag)).longValue();
		}
		return defaultValue;
	}
	
	public Double getDouble(String tag, Map<String, Object> valueMap) {
		return getDouble(tag, valueMap, null);
	}
	
	public Double getDouble(String tag, Map<String, Object> valueMap, Double defaultValue) {
		if (valueMap.get(tag) != null) {
			return ((Number)valueMap.get(tag)).doubleValue();
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
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("'" + stringDate + "' could not be converted to a long. ZonedDateTime could not be determined."); 
			}
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
	
	public String encodeBase64(String stringToEncode, int rounds) {
		if (stringToEncode == null) {
			return null;
		}
		Encoder encoder = Base64.getEncoder();
		byte[] encoded = stringToEncode.getBytes();
		for (int i=0; i < rounds; i++) {
			encoded = encoder.encode(encoded);
		}
		return new String(encoded);
	}
	
	public String decodeBase64(String stringToDecode, int rounds) {
		if (stringToDecode == null) {
			return null;
		}
		Decoder decoder = Base64.getDecoder();
		byte[] decoded = stringToDecode.getBytes();
		for (int i=0; i < rounds; i++) {
			decoded = decoder.decode(decoded);
		}
		return new String(decoded);
	}
	
}
