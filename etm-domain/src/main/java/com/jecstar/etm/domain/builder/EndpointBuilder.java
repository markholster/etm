package com.jecstar.etm.domain.builder;

import java.time.ZonedDateTime;

import com.jecstar.etm.domain.Endpoint;
import com.jecstar.etm.domain.EndpointHandler;

public class EndpointBuilder {

	private final Endpoint endpoint;

	public EndpointBuilder() {
		this.endpoint = new Endpoint();
	}
	
	public Endpoint build() {
		return this.endpoint;
	}
	
	public EndpointBuilder setName(String name) {
		this.endpoint.name = name;
		return this;
	}
	
	public String getName() {
		return this.endpoint.name;
	}
	
	public EndpointBuilder setWritingEndpointHandler(EndpointHandler writingEndpointHandler) {
		this.endpoint.writingEndpointHandler = writingEndpointHandler;
		return this;
	}
	
	public EndpointBuilder setWritingEndpointHandler(EndpointHandlerBuilder writingEndpointHandlerBuilder) {
		this.endpoint.writingEndpointHandler = writingEndpointHandlerBuilder.build();
		return this;
	}
	
	public EndpointBuilder addReadingEndpointHandler(EndpointHandler readingEndpointHandler) {
		this.endpoint.readingEndpointHandlers.add(readingEndpointHandler);
		return this;
	}

	public EndpointBuilder addReadingEndpointHandler(EndpointHandlerBuilder readingEndpointHandlerBuilder) {
		this.endpoint.readingEndpointHandlers.add(readingEndpointHandlerBuilder.build());
		return this;
	}
	
	public EndpointBuilder setWritingTimeToNow() {
		if (this.endpoint.writingEndpointHandler != null) {
			this.endpoint.writingEndpointHandler.handlingTime = ZonedDateTime.now();
		}
		return this;
	}
}
