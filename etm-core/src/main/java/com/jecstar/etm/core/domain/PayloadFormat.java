package com.jecstar.etm.core.domain;

public enum PayloadFormat {

	HTML,
	JSON,
	SOAP,
	SQL,
	TEXT,
	XML;
	
	public static PayloadFormat safeValueOf(String value) {
		if (value == null) {
			return null;
		}
		try {
			return PayloadFormat.valueOf(value.toUpperCase());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
