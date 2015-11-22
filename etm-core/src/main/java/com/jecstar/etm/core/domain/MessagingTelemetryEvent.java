package com.jecstar.etm.core.domain;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MessagingTelemetryEvent extends TelemetryEvent<MessagingTelemetryEvent> {
	
	public enum MessagingEventType {
		
		REQUEST, RESPONSE, FIRE_FORGET;
		
		public static MessagingEventType saveValueOf(String value) {
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
	
	/**
	 * The handlers that were reading the event.
	 */
	public List<EndpointHandler> readingEndpointHandlers = new ArrayList<EndpointHandler>();
	

	@Override
	public MessagingTelemetryEvent initialize() {
		super.internalInitialize();
		this.expiry = null;
		this.messagingEventType = null;
		this.readingEndpointHandlers.clear();
		return this;
	}

	@Override
	public MessagingTelemetryEvent initialize(MessagingTelemetryEvent copy) {
		super.internalInitialize(copy);
		this.initialize();
		this.expiry = copy.expiry;
		this.messagingEventType = copy.messagingEventType;
		this.readingEndpointHandlers.addAll(copy.readingEndpointHandlers);
		return this;
	}
	
	@Override
	public ZonedDateTime getInternalEventTime() {
		Optional<EndpointHandler> optional = this.readingEndpointHandlers.stream().sorted((h1, h2) -> h1.handlingTime.compareTo(h2.handlingTime)).findFirst();
		if (optional.isPresent()) {
			return optional.get().handlingTime;
		}
		return super.getEventTime();
	}
}
