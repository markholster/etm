package com.jecstar.etm.core.domain;

import java.time.ZonedDateTime;

public class MessagingTelemetryEvent extends TelemetryEvent<MessagingTelemetryEvent> {
	
	public enum MessagingEventType {
		
		REQUEST, RESPONSE, FIRE_FORGET;
		
		public static MessagingEventType safeValueOf(String value) {
			if (value == null) {
				return null;
			}
			try {
				return MessagingEventType.valueOf(value.toUpperCase());
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
	}
	
	/**
	 * The moment this event expires, in case of a request.
	 */
	public ZonedDateTime expiry;
	
	/**
	 * The messaging type that this event represents.
	 */
	public MessagingEventType messagingEventType;	

	@Override
	public MessagingTelemetryEvent initialize() {
		super.internalInitialize();
		this.expiry = null;
		this.messagingEventType = null;
		return this;
	}

	@Override
	public MessagingTelemetryEvent initialize(MessagingTelemetryEvent copy) {
		super.internalInitialize(copy);
		this.expiry = copy.expiry;
		this.messagingEventType = copy.messagingEventType;
		return this;
	}
}
