package com.jecstar.etm.core.domain;

public enum Transport {

	/**
	 * Enum constant representing a file system as the transport type of the payload.
	 */
	FILE_SYSTEM, 	
	/**
	 * Enum constant representing FTP as the transport type of the payload.
	 */
	FTP, 
	/**
	 * Enum constant representing HTTP as the transport type of the payload.
	 */
	HTTP, 
	/**
	 * Enum constant representing JDBC as the transport type of the payload.
	 */
	JDBC,
	/**
	 * Enum constant representing ODBC as the transport type of the payload.
	 */
	ODBC,
	/**
	 * Enum constant representing MQ as the transport type of the payload.
	 */
	MQ, 
	/**
	 * Enum constant representing SMTP as the transport type of the payload.
	 */
	SMTP;

	public static Transport saveValueOf(String value) {
		if (value == null) {
			return null;
		}
		try {
			return Transport.valueOf(value.toUpperCase());
		} catch (IllegalArgumentException e) {
			return null;
		}
	} 
}
