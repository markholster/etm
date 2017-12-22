package com.jecstar.etm.domain;

public class SqlTelemetryEvent extends TelemetryEvent<SqlTelemetryEvent>{

	public enum SqlEventType {
		DELETE, INSERT, SELECT, UPDATE, RESULTSET;
		
		public static SqlEventType safeValueOf(String value) {
			if (value == null) {
				return null;
			}
			try {
				return SqlEventType.valueOf(value.toUpperCase());
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
	}
	
	/**
	 * The query event type that is represented by this event.
	 */
	public SqlEventType sqlEventType;

	@Override
	public SqlTelemetryEvent initialize() {
		this.sqlEventType = null;
		return this;
	}

	@Override
	public SqlTelemetryEvent initialize(SqlTelemetryEvent copy) {
		super.internalInitialize(copy);
		this.sqlEventType = copy.sqlEventType;
		return this;
	}
}
